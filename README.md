## NullAwayAnnotator  ![Build Status](https://github.com/nimakarimipour/NullAwayAnnotator/actions/workflows/continuous-integration.yml/badge.svg)
```NullAwayAnnotator``` or simply (Annotator) is a tool that can automatically infer types in source code and injects the 
corresponding annotations to pass [NullAway](https://github.com/uber/NullAway) checks.

Applying NullAway to build systems is a tedious task. It requires a lot of manual work to annotate the source code. 
Even if a code free of nullability errors, it is still required to annotate the code to pass NullAway checks. 
A tool that can automatically infer types in source code and injects the corresponding annotations to pass NullAway checks, can significantly reduce the effort of applying NullAway to build systems.

```Annotator``` minimizes the number nullaway reported errors by inferring nullability types of elements in the source code and injects 
the corresponding annotations. For errors that are not resolvable with any annotations, annotator injects appropriate suppression annotations.
The final output of Annotator, is a source code that passes NullAway checks leaving no errors.

## Code Example

In the code below, ```NullAway``` will report five warnings.

```java
class Test{
    Object f1 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f2 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f3 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f4 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f5 = f4;
    Object f6 = new Object();
    
    String m1(){
        return f1 != null ? f1.toString() : f2.toString() + f6.toString();
    }
    
    int m2(){
        return f3 != null ? f3.hashCode() : f2.hashCode() + f6.hashCode();
    }
    
    Object m3(){
        return f5;
    }
    
    void m4(){
         f6 = null; // warning: assigning @Nullable expression to @NonNull field
    }
}
```

```Annotator``` can infer the nullable types in the code above and injects the corresponding annotations. For unresolved errors, suppression annotations are injected.
The output below is the result of running ```Annotator``` on the code above.

```java
import javax.annotation.Nullable; // added by Annotator
import org.jspecify.annotations.NullUnmarked; // added by Annotator

class Test{
    @Nullable Object f1 = null;
    @SuppressWarnings("NullAway") Object f2 = null; // inferred to be @Nonnull, and null assignment is suppressed.
    @Nullable Object f3 = null;
    @Nullable Object f4 = null;
    @Nullable Object f5 = f4;
    Object f6 = new Object();  // inferred to be @Nonnull
    
    String m1(){
        return f1 != null ? f1.toString() : f2.toString() + f6.toString();
    }
    
    int m2(){
        return f3 != null ? f3.hashCode() : f2.hashCode() + f6.hashCode();
    }
    
    @Nullable Object m3(){ // inferred to be @Nullable as a result of f5 being @Nullable.
        return f5;
    }
    
    @NullUnmarked //f6 is inferred to be @Nonnull, but it is assigned to null. The error is suppressed by @NullUnmarked.
    void m4(){
         f6 = null; 
    }
}
```

```Annotator``` propagates effects of a change through the entire module and injects several followups annotations to fully resolve one specific warning.
Annotator is also capable of processing modules within monorepos considering modules public APIs and the impacts of annotations on downstream dependencies for better results.

## Installation
No installation is required, annotator jar file is provided via maven central repository. Find the artifact id below:
```
GROUP: edu.ucr.cs.riple.annotator
ID: annotator-core
```

## Running Annotator

Please read the instruction [here](runner/README.md) to run the annotator.
