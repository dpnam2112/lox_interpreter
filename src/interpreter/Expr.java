package interpreter;
import java.util.List;

public abstract class Expr{
 public static interface Visitor <R> {

  public R visitBinary(Binary expr);
  public R visitUnary(Unary expr);
  public R visitGrouping(Grouping expr);
  public R visitLiteral(Literal expr);
  public R visitTernary(Ternary expr);
  public R visitVariable(Variable expr);
  public R visitAssign(Assign expr);
  public R visitCall(Call expr);
  public R visitGet(Get expr);
  public R visitSet(Set expr);
  public R visitThis(This expr);
  public R visitFunction(Function expr);
 }

 public static class Binary extends Expr {
  public final Expr left;
  public final Expr right;
  public final Token op;

  public Binary(Expr left, Expr right, Token op) {
   this.left = left;
   this.right = right;
   this.op = op;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitBinary(this);
  }
 }

 public static class Unary extends Expr {
  public final Token op;
  public final Expr exp;

  public Unary(Token op, Expr exp) {
   this.op = op;
   this.exp = exp;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitUnary(this);
  }
 }

 public static class Grouping extends Expr {
  public final Expr exp;

  public Grouping(Expr exp) {
   this.exp = exp;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitGrouping(this);
  }
 }

 public static class Literal extends Expr {
  public final Object litValue;

  public Literal(Object litValue) {
   this.litValue = litValue;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitLiteral(this);
  }
 }

 public static class Ternary extends Expr {
  public final Expr condition;
  public final Expr ifTrue;
  public final Expr ifFalse;

  public Ternary(Expr condition, Expr ifTrue, Expr ifFalse) {
   this.condition = condition;
   this.ifTrue = ifTrue;
   this.ifFalse = ifFalse;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitTernary(this);
  }
 }

 public static class Variable extends Expr {
  public final Token name;

  public Variable(Token name) {
   this.name = name;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitVariable(this);
  }
 }

 public static class Assign extends Expr {
  public final Token name;
  public final Expr value;
  public final Token op;

  public Assign(Token name, Expr value, Token op) {
   this.name = name;
   this.value = value;
   this.op = op;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitAssign(this);
  }
 }

 public static class Call extends Expr {
  public final Expr callee;
  public final Token paren;
  public final List<Expr> args;

  public Call(Expr callee, Token paren, List<Expr> args) {
   this.callee = callee;
   this.paren = paren;
   this.args = args;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitCall(this);
  }
 }

 public static class Get extends Expr {
  public final Expr object;
  public final Token field;

  public Get(Expr object, Token field) {
   this.object = object;
   this.field = field;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitGet(this);
  }
 }

 public static class Set extends Expr {
  public final Expr object;
  public final Token field;
  public final Expr value;

  public Set(Expr object, Token field, Expr value) {
   this.object = object;
   this.field = field;
   this.value = value;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitSet(this);
  }
 }

 public static class This extends Expr {
  public final Token keyword;

  public This(Token keyword) {
   this.keyword = keyword;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitThis(this);
  }
 }

 public static class Function extends Expr {
  public final Stmt.Block body;
  public final List<Token> args;

  public Function(Stmt.Block body, List<Token> args) {
   this.body = body;
   this.args = args;
  }

  public <R> R accept(Visitor<R> visitor) {
   return visitor.visitFunction(this);
  }
 }


 public abstract <R> R accept(Visitor<R> visitor);
}