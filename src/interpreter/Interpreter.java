package interpreter;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment global = new Environment();
	Environment environment = global;
	
	/* locals: Keep track of 'depth' of each variable */
	private final Map<Expr, Integer> locals = new HashMap<>();
	
	Interpreter() {
		global.define("clock", new Callable() {
			public int arity() {
				return 0;
			}
			
			public Object call(Interpreter interpreter, List<Object> args) {
				return (double) System.currentTimeMillis() / 1000.0;
			}
			
			public String toString() {
				return "<native fn>";
			}
		});
	}

	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}
	
	private void checkNumberOperand(Token token, Object operand) {
		if (operand instanceof Double) {
			return;
		}
		
		throw new RuntimeError(token, "Missing a numeric operand");
	}
	
	private void checkNumberOperands(Token token, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) {
			return;
		}
		
		throw new RuntimeError(token, "Both operands must be numeric");
	}
	
	private boolean truthVal(Object literal) {
		/* We treat `false` and `nil` objects as `false`
		 * */
		if (literal == null)
			return false;
		if (literal instanceof Boolean)
			return (boolean) literal;
		return true;
	}
	
	private boolean isEqual(Object leftVal, Object rightVal) {
		if (leftVal == null && rightVal == null) {
			return true;
		}
		else if (leftVal == null || rightVal == null) {
			return false;
		}
		
		return leftVal.equals(rightVal);
	}
	
	private String stringify(Object object) {
		if (object == null) {
			return "nil";
		}
		
		return object.toString();
	}
	
	public void interpret(List<Stmt> stmts) {
		try {
			for (Stmt stmt : stmts) {
				execute(stmt);
			}
		}
		catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}
	
	private void execute(Stmt stmt) {
		if (stmt != null)
			stmt.accept(this);
	}
	
	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}
	
	Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		
		if (distance == null) {
			return environment.get(name);
		}
		
		return environment.ancestor(distance).values.get(name.lexeme);
	}
	
	void executeBlock(Stmt.Block block, Environment env) {
		this.environment = env;
		
		try {
			for (Stmt statement : block.statements) {
				execute(statement);
			}
		}
		finally {
			this.environment = env.outerEnv;
		}
	}
	
	/* Implement Visitor interface for Expr types */
	public Object visitLiteral(Expr.Literal expr) {
		return expr.litValue;
	}
	
	public Object visitVariable(Expr.Variable expr) {
		//return environment.get(expr.name);

		Integer depth = locals.get(expr);
		if (depth == null) {
			return global.get(expr.name);
		}
		return environment.ancestor(depth).get(expr.name);
	}
	
	public Object visitUnary(Expr.Unary expr) {
		Object rightVal = evaluate(expr.exp);
		
		switch (expr.op.type) {
		case MINUS:
			checkNumberOperand(expr.op, rightVal);
			return 0 - (double) rightVal;
		}
		
		return !truthVal(rightVal);
	}
	
	public Object visitBinary(Expr.Binary expr) {
		Object leftVal = evaluate(expr.left);
		
		/* Handle cases that the expression is either a conjunction or disjunction
		 * 
		 * Sometimes, we do not need to evaluate both sides of conjunctions or disjunctions.
		 * E.g, in the expression f(a) || f(b), if f(a) returns a `true`, then the value of
		 * this expression is `true`, there is no need to evaluate f(b).
		 * 
		 * Because of that reason, we handle conjunctions and disjunctions separately from the others. */

		if (expr.op.type == TokenType.AND) {
			 if (truthVal(leftVal))
				 return evaluate(expr.right);
			 return leftVal;
		}
		else if (expr.op.type == TokenType.OR) {
			if (truthVal(leftVal))
				return leftVal;
			return evaluate(expr.right);
		}
		
		/* Evaluate expressions in which both sides need to be evaluated. */
		
		Object rightVal = evaluate(expr.right);
		
		switch (expr.op.type) {
		case PLUS:
			if (leftVal instanceof Double && rightVal instanceof Double) {
				return (double) leftVal + (double) rightVal;
			}
			
			if (leftVal instanceof String && rightVal instanceof String) {
				return (String) leftVal + (String) rightVal;
			}

			throw new RuntimeError(expr.op, "Both operands must be either strings or numerics");
		case MINUS:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal - (double) rightVal;
		case STAR:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal * (double) rightVal;
		case SLASH:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal / (double) rightVal;
		case MOD:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal % (double) rightVal;
		case EQ:
			return isEqual(leftVal, rightVal);
		case DIFF:
			return !isEqual(leftVal, rightVal);
		case LT:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal < (double) rightVal;
		case GT:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal > (double) rightVal;
		case LT_EQ:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal >= (double) rightVal;
		case GT_EQ:
			checkNumberOperands(expr.op, leftVal, rightVal);
			return (double) leftVal <=(double) rightVal;
		case COMMA:
			return rightVal;
		}
		
		return null;
	}
	
	public Object visitGrouping(Expr.Grouping expr) {
		return evaluate(expr.exp);
	}
	
	public Object visitTernary(Expr.Ternary expr) {
		Object condition = evaluate(expr.condition);
		Object ifTrueExp = evaluate(expr.ifTrue);
		
		if (isEqual(condition, true)) {
			return ifTrueExp;
		}
		
		return evaluate(expr.ifFalse);
	}

	public Object visitAssign(Expr.Assign expr) {
		Token name = expr.name;
		Object value = evaluate(expr.value);

		Integer depth = locals.get(expr);

		/* Evaluate new value of variable */
		Object currentVal;
		if (depth == null) {
			currentVal = global.get(name);
		}
		else {
			currentVal = environment.getAt(depth, name);
		}

		switch (expr.op.type) {
		case ASGN:
			currentVal = value;
			break;
		case INC_ASGN:
			if (value instanceof String && currentVal instanceof String) {
				currentVal = (String) currentVal + (String) value;
			}
			else if (value instanceof Double && currentVal instanceof Double) {
				currentVal = (double) currentVal + (double) value;
			}
			else {
				throw new RuntimeError(expr.op, "Both operands must be either strings or numerics.");				
			}		
			break;
		case DEC_ASGN:
			checkNumberOperands(expr.op, value, currentVal);
			currentVal = (double) currentVal - (double) value;
		}

		if (depth == null)
			global.assign(name, currentVal);
		else
			environment.assignAt(depth, name, currentVal);
		
		return currentVal;
	}
	
	public Object visitCall(Expr.Call call) {
		Object callee = evaluate(call.callee);
		
		if (!(callee instanceof Callable)) {
			throw new RuntimeError(call.paren, "The expression before '(' is not callable.");
		}
		
		/* Evaluate each expression argument */
		List<Object> arguments = new ArrayList<>();
		for (Expr arg : call.args) {
			arguments.add(evaluate(arg));
		}
		
		Callable function = (Callable) callee;
		
		/* Validate number of arguments */
		if (function.arity() != arguments.size()) {
			String message = "Expect " + function.arity() + " arguments but found " + arguments.size() + " arguments.";
			throw new RuntimeError(call.paren, message);
		}
		
		return function.call(this, arguments);
	}
	
	public Object visitGet(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (!(object instanceof Instance)) {
			 throw new RuntimeError(expr.field, "Invalid field access.");
		}
		 
		return ((Instance) object).get(expr.field);
	}

	public Object visitThis(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
	}
	
	public Object visitSet(Expr.Set expr) {
		Object instance = evaluate(expr.object);
		if (!(instance instanceof Instance)) {
			throw new RuntimeError(expr.field, "Only objects have properties.");
		}
		Object value = evaluate(expr.value);
		((Instance) instance).set(expr.field, value);
		return value;
	}

	public Object visitFunction(Expr.Function expr) {
		Stmt.FuncStmt declaration = new Stmt.FuncStmt(null, expr.body, expr.args);
		return new Function(declaration, environment);
	}

	/* Implement Visitor interface for statements */
	
	public Void visitExpression(Stmt.Expression stmt) {
		Object val = evaluate(stmt.expression);
		if (Lox.consoleMode() && environment.outerEnv == null) {
			System.out.println(stringify(val));
		}
		return null;
	}
	
	public Void visitPrint(Stmt.Print stmt) {
		Object exprVal = evaluate(stmt.expression);
		System.out.println(stringify(exprVal));
		return null;
	}
	
	public Void visitVarStmt(Stmt.VarStmt stmt) {
		environment.define(stmt.identifier, evaluate(stmt.init));
		return null;
	}

	public Void visitBlock(Stmt.Block block) {
		executeBlock(block, new Environment(this.environment));
		return null;
	}
	
	public Void visitConditional(Stmt.Conditional conditional) {
		if (truthVal(evaluate(conditional.expr)))
			execute(conditional.thenBranch);
		else if (conditional.elseBranch != null)
			execute(conditional.elseBranch);
		
		return null;
	}
	
	public Void visitWhile(Stmt.While whileStmt) {
		while (truthVal(evaluate(whileStmt.expr))) {
			try {
				execute(whileStmt.body);
				
				/* If the loop is a for-loop, increment expression != null */
				if (whileStmt.increment != null)
					evaluate(whileStmt.increment);
			}
			catch (LoopException exp) {
				if (exp.token.type == TokenType.BREAK)
					break;
				if (whileStmt.increment != null)
					evaluate(whileStmt.increment);
				continue;
			}
		}
		
		return null;
	}
	
	public Void visitFuncStmt(Stmt.FuncStmt funcStmt) {
		environment.define(funcStmt.name, new Function(funcStmt, environment));
		return null;
	}
	
	public Void visitReturn(Stmt.Return returnStmt) {
		Object value = null;
		
		if (returnStmt.expr != null)
			value = evaluate(returnStmt.expr);
		
		throw new ReturnValue(value);
	}
	
	public Void visitJump(Stmt.Jump stmt) {
		throw new LoopException(stmt.token);
	}
	
	public Void visitClass(Stmt.Class stmt) {
		// Validate the superclass
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass)) {
				throw new RuntimeError(stmt.superclass.name,
						"'" + stmt.superclass.name.lexeme + "' is not a class.");
			}
		}

		environment.define(stmt.name, null);
		
		if (superclass != null) {
			/* Create a closure for each method in the inherited class */
			environment = new Environment(environment);
			environment.define("super", superclass);
		}

		Map<String, Function> methodMap = new HashMap<>();
		for (Stmt.FuncStmt method : stmt.methods) {
			Function methodObj = new Function(method, environment, method.name.lexeme.equals("init"));
			methodMap.put(method.name.lexeme, methodObj);
		}
		
		if (superclass != null)
			environment = environment.outerEnv;
		
		LoxClass classObj = new LoxClass(stmt.name.lexeme, methodMap, (LoxClass) superclass);
		environment.assign(stmt.name, classObj);

		return null;
	}
	
	public Object visitSuper(Expr.Super expr) {
		int spDistance = locals.get(expr);
		LoxClass superclass = (LoxClass) environment.getAt(spDistance, expr.keyword);
		Instance instance = (Instance) environment.getAt(spDistance - 1, "this");
		Function method = superclass.findMethod(expr.method.lexeme);
		if (method == null) {
			throw new RuntimeError(expr.keyword, "The superclass does not own method '"
									+ expr.method.lexeme + "'.");
		}
		return method.bind(instance);
	}
}
