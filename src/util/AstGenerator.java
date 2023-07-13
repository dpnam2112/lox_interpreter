package util;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

public class AstGenerator {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: AstGenerator <path to output>");
			System.exit(1);
		}

		String[] externalClasses = {
				"java.util.List",
		};
		
		final String dest = args[0];
		
		/* AST definitions */
		String[] ASTDefs = {
				"Binary: Expr left, Expr right, Token op",
				"Unary: Token op, Expr exp",
				"Grouping: Expr exp",
				"Literal: Object litValue",
				"Ternary: Expr condition, Expr ifTrue, Expr ifFalse",
				"Variable: Token name",
				"Assign: Token name, Expr value, Token op",
				"Call: Expr callee, Token paren, List<Expr> args",
				"Get: Expr object, Token field",
				"Set: Expr object, Token field, Expr value",
				"This: Token keyword",
				"Function: Stmt.Block body, List<Token> args",
		};
		
		defineAST(dest, "Expr", ASTDefs, externalClasses);
		
		String[] SSTDefs = {
				"Expression: Expr expression",
				"Print: Expr expression",
				"VarStmt: Token identifier, Expr init",
				"Block: List<Stmt> statements",
				"Conditional: Expr expr, Stmt thenBranch, Stmt elseBranch",
				"While: Expr expr, Stmt body, Expr increment",
				"FuncStmt: Token name, Block body, List<Token> argNames",
				"Return: Token keyword, Expr expr",
				"Jump: Token token",
				"Class: Token name, List<Stmt.FuncStmt> methods",
		};
		

		
		defineAST(dest, "Stmt", SSTDefs, externalClasses);
	}
	
	private static void defineAST(String dest, String className, String[] types, String[] externalClasses) throws IOException {
		/* fieldStr' format: <type> field_1, <type> field_2, ... */
		final FileWriter writer = new FileWriter(dest + "/" + className + ".java");

		writer.write("package interpreter;\n");

		if (externalClasses != null) {
			for (String externalClass :  externalClasses) {
				writer.write("import " + externalClass + ";\n");
			}	
		}
		
		writer.write("\n");
		
		writer.write("public abstract class " + className + "{\n");

		defineVisitor(writer, "Visitor", types);
		
		writer.write("\n");
		
		for (String typeDef : types) {
			String typeName = typeDef.split(":")[0].trim();
			String fields = typeDef.split(":")[1].trim();
			
			defineType(writer, typeName, className, fields);
			
			writer.write("\n");
		}
		
		writer.write("\n");
		
		writer.write(" public abstract <R> R accept(Visitor<R> visitor);\n");
		
		writer.write("}");
		
		writer.close();
	}
	
	private static void defineType(FileWriter writer, String className, String baseClass, String fieldStr) throws IOException {
		/* fieldStr' format: <type> field_1, <type> field_2, ... */
		writer.write(" public static class " + className + " extends " + baseClass + " {\n");
		
		String[] fieldList = fieldStr.split(", ");
		
		/* Field declaration */
		for (int i = 0; i < fieldList.length; i++) {
			String fieldType = fieldList[i].split(" ")[0].trim();
			String fieldName = fieldList[i].split(" ")[1].trim();
			
			writer.write("  public final " + fieldType + " " + fieldName + ";\n");
		}
		
		writer.write("\n");
		
		/* write constructor */
		writer.write("  public " + className + "(" + fieldStr + ") {\n");

		/* Field initialization */
		for (int i = 0; i < fieldList.length; i++) {
			String fieldName = fieldList[i].split(" ")[1].trim();
			writer.write("   this." + fieldName + " = " + fieldName + ";\n");
		}
		
		
		writer.write("  }\n");
		writer.write("\n");

		/* accept() method for visitor pattern */
		writer.write("  public <R> R accept(Visitor<R> visitor) {\n");
		writer.write("   return visitor.visit" + className + "(this);\n");
		
		writer.write("  }\n");
		writer.write(" }\n");
	}
	
	private static void defineVisitor(FileWriter writer, String className, String[] types) throws IOException {
		writer.write(" public static interface " + className + " <R> {\n");
		writer.write("\n");
		
		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.write("  public R visit" + typeName + "(" + typeName + " expr);\n");
		}
		
		writer.write(" }\n");
	}
}
