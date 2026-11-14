# CLAUDE.md

@AGENTS.md

# Engineering Principles

## 1. Think Before Coding

- Think through the problem before writing code.
- State your assumptions explicitly before implementation.
- If a requirement is ambiguous, ask for clarification rather than guessing.
- If a simpler approach exists, explain it and challenge unnecessary complexity.
- If something is unclear, stop and explain exactly what is unclear.
- Never silently choose an interpretation and proceed.

## 2. Simplicity First

- Write the minimum code that correctly solves the problem.
- Avoid speculative abstractions, unnecessary flexibility, and premature generalization.
- Do not introduce design patterns unless they solve an actual design problem.
- Prefer straightforward, readable implementations.
- Before introducing an abstraction, ask: Would a senior engineer consider this necessary or overengineered?

## 3. Surgical Changes

- Modify only what the requested task requires.
- Do not refactor neighboring code or fix unrelated issues.
- Do not introduce unrelated formatting changes.
- Preserve existing architectural decisions unless the task explicitly requires changing them.
- Every changed line must have a clear reason connected to the request.

## 4. Goal-Driven Execution

- Convert vague requirements into concrete, verifiable outcomes before implementation.
- Identify expected behaviour, edge cases, and acceptance criteria.
- For validation-related changes, identify invalid inputs and expected outcomes before implementing validation.
- Prefer test-driven development where appropriate.
- Verify that the implementation satisfies the original requirement.

# Code Quality & Conventions

## 5. Logging

- Always use the project's configured logger. Never use `System.out`, `System.err`, `printStackTrace()`, or direct console output for application logging.
- Write every logging statement on a single physical line. Never split a logging statement across multiple lines.
- Follow the existing logging framework and logging conventions.

## 6. Comments & Class Documentation

- Add a class-level description of no more than three lines when it provides meaningful information about the class's responsibility or purpose.
- Do not add comments merely to explain obvious code.
- Avoid redundant comments, unnecessary inline comments, and comments that simply repeat method or variable names.
- Add comments only when they clarify non-obvious logic, important constraints, design decisions, or reasoning that cannot be understood easily from the code.
- Prefer self-explanatory names and readable code over excessive comments.

# Architecture & Communication

## 7. Architectural Visibility

After completing a file or a coherent implementation change, provide an architectural explanation.

Include:
- The responsibility of each affected class.
- The design patterns used, if any, and why they are appropriate.
- How the affected classes interact with existing components.
- The execution flow through the relevant classes.

Use a Mermaid flowchart when multiple classes or components interact. For a single isolated class, a concise explanation is sufficient.

Before presenting a PR review, provide an architectural overview of the changes and explain how they fit into the existing architecture.

Do not invent design patterns merely to make the explanation look sophisticated. Explicitly state when no design pattern is necessary.

## 8. Test Workflow Visualization

For every test suite or meaningful test scenario, provide a Mermaid flowchart showing:

- The workflow or use case being tested.
- The entry point of the workflow.
- Which classes and methods participate.
- Where the test assertions or expected outcomes occur.
- Which parts of the workflow are covered and which are not.

Make it clear what actual application behaviour the test validates.

Do not present a list of test methods as a substitute for explaining the tested workflow.

# Execution & Delegation Rules

## 9. Pre-Implementation Brief

Before implementing a feature or a significant change:

1. Understand the existing implementation and relevant architecture.
2. Identify the files likely to change.
3. Explain the proposed approach and execution flow.
4. Identify relevant design patterns, trade-offs, and potential risks.
5. Identify the tests and verification strategy.

For `/implement-spec` specifically:

- Never spawn background subagents that write or modify code before presenting the Pre-Implementation Brief in the main conversation.
- Present the complete brief in the main conversation first.
- Wait for my explicit approval before starting implementation through background coding subagents.
- Background research or read-only investigation is permitted, provided it does not modify project files.

## 10. Git Operations & Branch Management

- Never create a feature branch. Always work on the currently checked-out branch.
- After completing a milestone (a coherent set of changes that compiles and passes checks), commit and raise a PR automatically — do not wait for permission.
- Never amend, reset, rebase, force-push, or otherwise rewrite Git history without asking first.
- Write clear commit messages that describe what changed and why.

# Final Implementation Report

After completing an implementation task, provide:

1. **What changed:** Files modified and their responsibilities.
2. **Design:** Architectural flow and applicable design patterns.
3. **Tests:** Test workflows, preferably illustrated using Mermaid.
4. **Verification:** Commands executed and their results.
5. **Limitations:** Any untested behaviour, assumptions, or outstanding concerns.

Do not claim that tests passed unless they were actually executed successfully.
