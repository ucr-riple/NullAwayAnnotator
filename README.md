## NullAwayAnnotator
```NullAwayAnnotator``` or simply (Annotator) is a tool that can automatically infer types in source code and injects the 
corresponding annotations to pass [NullAway](https://github.com/uber/NullAway) checks.

```Annotator``` is fast, it benefits from a huge parallelization technique to deliver the final product. On average, 
it is capable of reducing the number of warnings reported by ```NullAway``` down to 30%. Annotations are directly injected to the source code, 
and it preserves the code style.

## Code Example

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

```Annotator``` can automatically infer ```nullableFoo``` to be ```@Nullable``` and ```nonnullFoo``` to be ```@Nonnull```.
Therefore, it makes ```nullableFoo```, ```@Nullable``` and leave ```nonnullFoo``` untouched.

Below is the output of running Annotator on the code above:

```java
import javax.annotation.Nullable;

public class Test {
    Object bar = new Object();
    @Nullable Object nullableFoo; // resolved by Annotator
    Object nonnullFoo; //warning: "nonnullFoo" is not initialized (Annotator will not make it Nullable since the usage of this field demonstrates the programmer assumed it is @Nonnull).

    public @Nullable Object run(boolean check) {
        if (check) {
            return new Object();
        }
        return null; // resolved by Annotator
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

```Annotator``` propagates effects of a change through the entire module and injects several followups annotations to fully resolve one specific warning.
In the example below, making ```foo```, ```@Nullable``` requires two more ```@Nullable``` injections and ```Annotator``` automatically handles it.

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

```Annotator``` automatically follows the chain of warnings and finds the best solution using it's ```deep search``` technique. 
Below is the output of running ```Annotator``` in one run:

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

```Annotator``` requires subproject below to be installed in the ```local maven``` repository.
* [CSS](css/README.md): To install it, run `./gradlew install`

## Running Annotator

After following the installation instruction above, please read the instruction [here](runner/README.md) to run tha annotator.
