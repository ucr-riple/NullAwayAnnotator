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


## Installation

Run ```dependecies.sh``` to install the dependencies in maven local repository.
