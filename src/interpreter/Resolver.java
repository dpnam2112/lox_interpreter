package interpreter;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private final Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes;

	/* @currentFunc: Used to determine if the resolver is 'inside' a function/method declaration
	 * @currentClass: Used to determine if the resolver is 'inside' a class declaration 
	 * @isInLoop: Used to determine if the resolver is 'inside' a loop */
	private enum FunctionType {
		NONE, FUNCTION, METHOD, INIT,
	}
	private FunctionType currentFunc = FunctionType.NONE;
	
	private boolean isInLoop = false;

	private enum ClassType {
		NONE, CLASS, SUBCLASS,
	}
	private ClassType currentClass = ClassType.NONE;
	
	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
		this.scopes = new Stack<>();
	}
	
	public Void visitBlock(Stmt.Block block) {
		beginScope();
		resolve(block.statements);
		endScope();
		return null;
	}
	
	public Void visitVarStmt(Stmt.VarStmt stmt) {
		if (!scopes.empty() && scopes.peek().containsKey(stmt.identifier.lexeme)) {
			Lox.error(stmt.identifier, "Redeclaration of variable.");
			Lox.hadSyntaxError = true;
		}

		declare(stmt.identifier);
		if (stmt.init != null)
			resolve(stmt.init);
		define(stmt.identifier);
		return null;
	}
	
	public Void visitFuncStmt(Stmt.FuncStmt stmt) {
		declare(stmt.name);
		define(stmt.name);
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}
	
	public Void visitExpression(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}
	
	public Void visitConditional(Stmt.Conditional stmt) {
		resolve(stmt.expr);
		resolve(stmt.thenBranch);
		
		if (stmt.elseBranch != null)
			resolve(stmt.elseBranch);
		return null;
	}
	
	public Void visitWhile(Stmt.While stmt) {
		resolve(stmt.expr);
		
		if (stmt.body != null)
			resolve(stmt.body);
		if (stmt.increment != null)
			resolve(stmt.increment);

		return null;
	}
	
	public Void visitPrint(Stmt.Print stmt) {
		resolve(stmt.expression);
		return null;
	}
	
	public Void visitReturn(Stmt.Return stmt) {
		if (currentFunc == FunctionType.NONE) {
			Lox.error(stmt.keyword, "'return' statement outside function definition.");
			Lox.hadSyntaxError = true;
		}
		else if (currentFunc == FunctionType.INIT) {
			Lox.error(stmt.keyword, "expect 'nil' as return value for the constructor.");
			Lox.hadSyntaxError = true;
		}

		resolve(stmt.expr);
		return null;
	}

	public Void visitClass(Stmt.Class stmt) {
		declare(stmt.name);
		define(stmt.name);
		
		if (stmt.superclass != null) {
			// Check if the class is inherited from itself
			if (stmt.superclass.name.lexeme.equals(stmt.name.lexeme)) {
				Lox.error(stmt.superclass.name, "A class is not allowed to inherit from itself.");
				Lox.hadSyntaxError = true;
			}
			else {
				beginScope();
				scopes.peek().put("super", true);

				resolve(stmt.superclass);
			}
		}

		beginScope();
		ClassType beforeDecl = currentClass;	// Save state before class declaration
		currentClass = (stmt.superclass == null) ? ClassType.CLASS : ClassType.SUBCLASS;

		scopes.peek().put("this", true);
		for (Stmt.FuncStmt method : stmt.methods) {
			FunctionType declaration = (method.name.lexeme.equals("init")) ?
							FunctionType.INIT : FunctionType.METHOD;
			resolveFunction(method, declaration);
		}

		currentClass = beforeDecl;
		endScope();
		
		if (stmt.superclass != null) {
			endScope();
		}

		return null;
	}
	
	public Void visitJump(Stmt.Jump stmt) {
		if (!isInLoop) {
			Lox.error(stmt.token, "'" + stmt.token.lexeme + "' outside loop.");
			Lox.hadSyntaxError = true;
		}
		
		return null;
	}
	
	public Void visitBinary(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	public Void visitTernary(Expr.Ternary expr) {
		resolve(expr.condition);
		resolve(expr.ifFalse);
		resolve(expr.ifTrue);
		return null;
	}
	
	public Void visitUnary(Expr.Unary expr) {
		resolve(expr.exp);
		Lox.hadSyntaxError = true;
		return null;
	}
	
	public Void visitLiteral(Expr.Literal expr) {
		return null;
	}
	
	public Void visitGrouping(Expr.Grouping expr) {
		resolve(expr.exp);
		return null;
	}
	
	public Void visitAssign(Expr.Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}
	
	public Void visitCall(Expr.Call expr) {
		resolve(expr.callee);
		
		for (Expr param : expr.args) {
			resolve(param);
		}
		
		return null;
	}
	
	public Void visitVariable(Expr.Variable variable) {
		if (!scopes.empty() && scopes.peek().get(variable.name.lexeme) == Boolean.FALSE) {
			Lox.error(variable.name, "Can't read local variable in its own initializer.");
			Lox.hadSyntaxError = true;
		}
		
		resolveLocal(variable, variable.name);
		return null;
	}
	
	public Void visitGet(Expr.Get expr) {
		resolve(expr.object);
		return null;
	}
	
	public Void visitSet(Expr.Set expr) {
		resolve(expr.object);
		resolve(expr.value);
		return null;
	}

	public Void visitFunction(Expr.Function expr) {
		beginScope();
		// Tell resolver it is inside a function's body
		FunctionType beforeEval = currentFunc;
		currentFunc = FunctionType.FUNCTION;

		for (Token argument : expr.args) {
			declare(argument);
			define(argument);
		}
		resolve(expr.body.statements);

		currentFunc = beforeEval;
		endScope();
		return null;
	}

	public Void visitThis(Expr.This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "'this' outside class declaration.");
			Lox.hadSyntaxError = true;
		}

		resolveLocal(expr, expr.keyword);
		return null;
	}
	
	public Void visitSuper(Expr.Super expr) {
		if (currentClass != ClassType.SUBCLASS) {
			Lox.error(expr.keyword, "Use of 'super' outside subclasses.");
			Lox.hadSyntaxError = true;
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}
	
	private void declare(Token name) {
		if (scopes.empty()) {
			return;
		}
		
		scopes.peek().put(name.lexeme, false);
	}
	
	private void define(Token name) {
		if (scopes.empty())
			return;
		
		scopes.peek().put(name.lexeme, true);
	}
	
	void resolve(List<Stmt> statements) {
		for (Stmt statement : statements) {
			resolve(statement);
		}
	}
	
	private void resolve(Stmt statement) {
		statement.accept(this);
	}
	
	private void resolve(Expr expr) {
		expr.accept(this);
	}
	
	private void resolveFunction(Stmt.FuncStmt function, FunctionType type) {
		FunctionType enclosingFunc = currentFunc;
		currentFunc = type;
		
		beginScope();
		
		//Resolve parameters
		for (Token param : function.argNames) {
			declare(param);
			define(param);
		}
		
		resolve(function.body.statements);
		
		endScope();

		currentFunc = enclosingFunc;
	}
	
	private void beginScope() {
		scopes.add(new HashMap<String, Boolean>());
	}
	
	private void endScope() {
		scopes.pop();
	}
	
}
