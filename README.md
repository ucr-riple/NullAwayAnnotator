# NullAwayAutoFixer
```NullAwayAutoFixer``` (or simply ```AutoFixer```) is a tool that can automatically infer types in source code and injects the 
corresponding annotations to pass [NullAway](https://github.com/uber/NullAway).

```AutoFixer``` is fast, it benefits from doing a huge parallelism to deliver the final product. On average, 
it is capable of reducing the number of warning produced by ```NullAway``` down to 30%. Annotations are directly injected to the source code 
and it preserves the code style.

# Code Example

In the code below, ```NullAway``` will generate three warnings

```java
public class Test{
    Object bar = new Object();
    Object nullableFoo; //warning: "nullableFoo" is not initialized
    Object nonnullFoo; //warning: "nonnullFoo" is not initialized
    
    public Object run(boolean check){
        if(check){
            return new Object();
        }
        return null; //warning: returning nullable from nonNull method
    }
    
    public void display(){
        if(nullableFoo != null){
            String name = nullableFoo.toString();
            Class<?> clazz = nullableFoo.getClass();
        }
        String name = nonnullFoo.toString();
        Class<?> clazz = nonnullFoo.getClass();
    }
}
```

```AutoFixer``` can automatically infer ```nullableFoo``` to be ```@Nullable``` and for ```nonnullFoo``` to be ```@Nonnull```.
Therefore, it makes ```nullableFoo```, ```@Nullable``` and leave ```nonnullFoo``` untouched.

Below is the output of running AutoFixer on the code above:

```java
import javax.annotation.Nullable;

public class Test {
    Object bar = new Object();
    @Nullable Object nullableFoo; // resolved by AutoFixer
    Object nonnullFoo; //warning: "nonnullFoo" is not initialized (AutoFixer will not make it Nullable since it produces a lot more errors).

    public @Nullable Object run(boolean check) {
        if (check) {
            return new Object();
        }
        return null; // resolved by AutoFixer
    }

    public void display() {
        if (nullableFoo != null) {
            String name = nullableFoo.toString();
            Class<?> clazz = nullableFoo.getClass();
        }
        String name = nonnullFoo.toString();
        Class<?> clazz = nonnullFoo.getClass();
    }
}
```

```AutoFixer``` propagate effect of a change through the entire module inject several annotations to fully resolve one specific warning.
In the example below, making ```foo```, ```@Nullable``` requires two more ```@Nullable``` and ```AutoFixer``` automatically takes care of that.

```java
public class Test{
    Object foo; //warning: "nullableFoo" is not initialized

    public Object run(){
        bar(foo); // if foo was @Nullable, we would have seen the warning: passing nullable to nonnull param
        return foo; // if foo was @Nullable, we would have seen the warning: returning nullable from non-null method
    }
    
    public void bar(Object foo){
        if(foo != null){
            String name = foo.toString(); 
        }
    }
}
```

However, ```AutoFixer``` automatically follows the chain of warnings and find the best solution using it's ```deep search``` technique.
And produces the code below in one run:

```java
import javax.annotation.Nullable;

public class Test {
    @Nullable
    Object foo; //warning: resolved

    @Nullable
    public Object run() {
        bar(foo); //warning: resolved
        return foo; //warning: resolved
    }

    public void bar(@Nullable Object foo) {
        if (foo != null) {
            String name = foo.toString();
        }
    }
}
```

## Installation

Run ```dependecies.sh``` to install the dependencies in maven local repository.
