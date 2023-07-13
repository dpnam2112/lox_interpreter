package interpreter;

public class PrintAST implements Expr.Visitor<String> {
	public String visitUnary(Expr.Unary expr) {
		TokenType opType = expr.op.type;
		String subExprStr = expr.exp.accept(new PrintAST());
		
		return "(" + opType + " " + subExprStr + ")";
	}
	
	public String visitBinary(Expr.Binary expr) {
		/* representations of left and right expressions */
		String leftRep = expr.left.accept(new PrintAST());
		String rightRep = expr.right.accept(new PrintAST());
		
		return "(" + expr.op.type + " " + leftRep + " " + rightRep + ")";
	}
	
	public String visitLiteral(Expr.Literal expr) {
		if (expr == null) {
			return "nil";
		}
		return expr.litValue.toString();
	}
	
	public String visitGrouping(Expr.Grouping expr) {
		return expr.exp.accept(new PrintAST());
	}
	
	public String visitTernary(Expr.Ternary expr) {
		String conditionStr = expr.condition.accept(new PrintAST());
		String ifTrueStr = expr.ifTrue.accept(new PrintAST());
		String ifFalseStr = expr.ifFalse.accept(new PrintAST());
		
		return "(TRN " + conditionStr + " " + ifTrueStr +  " " + ifFalseStr + ")";
	}
	
	public String visitVariable(Expr.Variable expr) {
		return "(" + expr.name.lexeme + ": var)";
	}

	public String visitAssign(Expr.Assign expr) {
		return "(" + expr.name.lexeme + " = " + expr.accept(this) + ")";
	}
	
	public String visitCall(Expr.Call call) {
		String paramsStr = "(";
		for (int i = 0; i < call.args.size(); i++) {
			paramsStr += call.args.get(i).accept(this);
			if (i != call.args.size()) {
				paramsStr += ",";
			}
		}
		return "(" + call.callee.accept(this) + paramsStr + ")";
	}

	public String visitGet(Expr.Get get) {
		return "(" + get.object.accept(this) + "." + get.field.lexeme + ")";
	}

	public String visitSet(Expr.Set set) {
		return "(" + set.object.accept(this) + "." + 
			set.field.lexeme + 
			" = " + set.value.accept(this) + ")";
	}

	public String visitFunction(Expr.Function expr) {
		return "<anonymous func>";
	}

	public String visitThis(Expr.This _this) {
		return "this";
	}
	
	public static void main(String[] args) {
		Expr.Unary expr1 = new Expr.Unary(new Token(TokenType.MINUS, "-", null, 0), new Expr.Literal(123));
		Expr.Unary expr2 = new Expr.Unary(new Token(TokenType.NOT, "!", null, 0), new Expr.Literal(false));
		Expr.Binary expr3 = new Expr.Binary(expr1, expr2, new Token(TokenType.PLUS, "+", null, 0));
		System.out.println(expr1.accept(new PrintAST()));
		System.out.println(expr2.accept(new PrintAST()));
		System.out.println(expr3.accept(new PrintAST()));
	}
}
