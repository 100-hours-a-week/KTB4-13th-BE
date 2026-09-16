# Code Style Conventions

## 1. General Guidelines

- Use the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) as the baseline style guide.
- This project's explicit formatting rules below take precedence over the baseline.
- Use the design checklist as review guidance, not numeric limits that require extra classes or methods. Apply it to the code being changed rather than unrelated code.

## 2. Design Review Checklist

### Is control flow easy to follow?

- Prefer early returns when they reduce nesting. Extract a method when the extracted operation has a clear responsibility.
- Use `if`/`else`, a simple ternary, or a `switch` expression when it makes the alternatives easier to understand. Avoid nested ternaries and deeply nested branches.

### Does a value object capture a domain rule?

- Introduce a value object when it owns an invariant, domain behavior, or a meaningful distinction that prevents mistakes.
- Primitive values and strings are appropriate for simple values and boundary DTOs. Do not wrap them solely to satisfy a checklist.

### Does a collection own domain behavior?

- Use a first-class collection when it owns collection-wide rules or behavior.
- A plain collection is sufficient for data transfer. Protect domain state from external mutation with immutable views or defensive copies where needed.

### Do fields and parameters belong together?

- Keep related state and behavior together. Split a class when it has separate responsibilities, not when its field count crosses a fixed threshold.
- Group parameters when they express a meaningful concept. Do not invent parameter objects solely to reduce a count.
- DTOs, records, JPA entities, and mappers may need several fields or parameters to represent their contracts.

### Does the domain control its state transitions?

- Use domain methods to enforce invariants and state transitions rather than public setters that bypass them.
- Read-only accessors are appropriate when callers need domain state. DTO accessors and framework-required entity access do not replace domain behavior.

### Do calls respect responsibility boundaries?

- Avoid reaching through another object's internal structure to perform its work; place that behavior with its owner.
- Fluent APIs, stream pipelines, DTO mapping, and assertion chains are allowed when readable. Counting dots does not establish coupling.

### Does each method or class have a clear responsibility?

- Keep each method or class focused on a coherent responsibility.
- Extract separate responsibilities when this improves understanding; do not split code solely to make it shorter.

## 3. Input Validation and Error Contracts

### Did you actively use Jakarta Validation for external input?

- Prefer `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Size`, and `@Pattern` from `jakarta.validation.constraints` for required values, ranges, lengths, and format constraints in Presentation DTOs.
- Enable validation with `@Valid` or `@Validated` on the Controller and follow the HTTP error contract in [`ApiExceptionHandler`](../../api/src/main/java/com/book/api/support/web/ApiExceptionHandler.java).
- Avoid duplicating the same simple HTTP input checks in DTO constructors. Commands and Domain objects must still enforce their own preconditions and invariants for non-HTTP callers.
- Use the technology-neutral [`BusinessException`](../../common/src/main/java/com/book/common/exception/BusinessException.java) contract for shared business errors; map them to HTTP responses in `api/support/web`.
- Rules that are difficult to express with annotations alone, such as null defaults, input normalization, conditions between multiple fields, and domain invariants, belong in the constructor, Application, or Domain layer.
- Verify new constraints with tests that confirm invalid HTTP input returns the expected status code and error response contract.

## 4. Formatting and Java Conventions

### Did you use four spaces for indentation?

- Use four spaces for each indentation level.
- Do not use tab characters.
- Use eight spaces for continuation indentation.

### Did you keep each line within 120 characters?

- Limit each line to 120 characters.

### Did you always use braces for control statements?

- Use braces for `if`, `for`, and `while`, even when the body contains one line.
- Place the opening brace on the same line as the control statement or declaration (K&R style).

### Did you avoid wildcard imports?

- Do not use `import *`.
- Keep static imports separate from regular imports.

### Does each Java file contain only one top-level class?

- Define only one top-level class, interface, enum, or record in each `.java` file.

### Did you keep overloaded methods together?

- Place overloaded methods next to one another.

### Did you declare one variable per statement?

- Declare only one variable in each declaration statement.

### Did you declare local variables close to their use?

- Declare local variables near the first statement that uses them.

### Did you follow Java naming conventions?

- Use `lowercase` for package names.
- Use `UpperCamelCase` for class, interface, enum, and record names.
- Use `lowerCamelCase` for method and field names.
- Use `UPPER_SNAKE_CASE` only for semantic constants.
- Do not treat every `static final` member as a constant.
- Use `userId` instead of `userID`.

### Did you use `@Override` wherever applicable?

- Add `@Override` whenever a method overrides or implements inherited behavior.

### Did you handle exceptions explicitly?

- Do not silently ignore exceptions.
- Handle, log, or propagate each exception appropriately.

### Did you access static members through the class name?

- Access static fields and methods through the class name, not through an instance.

### Did you avoid `finalize()`?

- Do not declare or use `finalize()`.

### Did you document public APIs?

- Add Javadoc to public APIs by default.

### Did you apply these rules to modern Java syntax?

- Apply the same style standards to modern Java features, including `switch` expressions, `record`, text blocks,
  and Markdown-formatted Javadoc.
