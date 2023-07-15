package interpreter;
import java.util.List;

public abstract class Stmt{
 public static interface Visitor <R> {

  public R visitExpression(Expression expr);
  public R visitPrint(Print expr);
  public R visitVarStmt(VarStmt expr);
  public R visitBlock(Block expr);
  public R visitConditional(Conditional expr);
  public R visitWhile(While expr);
  public R visitFuncStmt(FuncStmt expr);
  public R visitReturn(Return expr);
  public R visitJump(Jump expr);
  public R visitClass(Class expr);
 }

 public static class Expression extends Stmt {
  public final Expr expression;

  public Expression(Expr expression) {
   this.expression = expression;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitExpression(this);
  }
 }

 public static class Print extends Stmt {
  public final Expr expression;

  public Print(Expr expression) {
   this.expression = expression;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitPrint(this);
  }
 }

 public static class VarStmt extends Stmt {
  public final Token identifier;
  public final Expr init;

  public VarStmt(Token identifier, Expr init) {
   this.identifier = identifier;
   this.init = init;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitVarStmt(this);
  }
 }

 public static class Block extends Stmt {
  public final List<Stmt> statements;

  public Block(List<Stmt> statements) {
   this.statements = statements;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitBlock(this);
  }
 }

 public static class Conditional extends Stmt {
  public final Expr expr;
  public final Stmt thenBranch;
  public final Stmt elseBranch;

  public Conditional(Expr expr, Stmt thenBranch, Stmt elseBranch) {
   this.expr = expr;
   this.thenBranch = thenBranch;
   this.elseBranch = elseBranch;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitConditional(this);
  }
 }

 public static class While extends Stmt {
  public final Expr expr;
  public final Stmt body;
  public final Expr increment;

  public While(Expr expr, Stmt body, Expr increment) {
   this.expr = expr;
   this.body = body;
   this.increment = increment;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitWhile(this);
  }
 }

 public static class FuncStmt extends Stmt {
  public final Token name;
  public final Block body;
  public final List<Token> argNames;

  public FuncStmt(Token name, Block body, List<Token> argNames) {
   this.name = name;
   this.body = body;
   this.argNames = argNames;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitFuncStmt(this);
  }
 }

 public static class Return extends Stmt {
  public final Token keyword;
  public final Expr expr;

  public Return(Token keyword, Expr expr) {
   this.keyword = keyword;
   this.expr = expr;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitReturn(this);
  }
 }

 public static class Jump extends Stmt {
  public final Token token;

  public Jump(Token token) {
   this.token = token;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitJump(this);
  }
 }

 public static class Class extends Stmt {
  public final Token name;
  public final List<Stmt.FuncStmt> methods;
  public final Expr.Variable superclass;

  public Class(Token name, List<Stmt.FuncStmt> methods, Expr.Variable superclass) {
   this.name = name;
   this.methods = methods;
   this.superclass = superclass;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitClass(this);
  }
 }


 public abstract <R> R accept(Visitor<R> visitor);
}