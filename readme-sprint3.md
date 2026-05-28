# Kotlin Calculator

A graphical calculator written in Kotlin. It supports the basic operations found in common Apple and Samsung calculator apps, including arithmetic, percentages, powers, roots, trigonometry, logarithms, constants, factorials, parentheses, and memory controls. It also includes simple calculus tools for numeric derivatives and definite integrals.

## Instructions for Build and Use

Steps to run the software:

1. Install Kotlin if it is not already installed.
2. Open a terminal in this folder.
3. Run `kotlin calculator.main.kts` to open the graphical calculator.
4. Run `kotlin calculator.main.kts --cli` to use the command-line version.

Instructions for using the software:

1. Type a calculation or use the buttons, such as `12 + 7 * 3`, `sqrt(81)`, `sin(30)`, or `2^8`.
2. Use `deg` or `rad` to switch trigonometry angle modes.
3. Use `m+`, `m-`, `ms`, `mr`, and `mc` for memory.
4. Type `help` to see all commands.
5. Type `quit` to exit.

Instructions for using calculus:

1. In the graphical calculator, enter a function using `x`, such as `x^2 + 3*x`.
2. Enter an `x` value and click `d/dx` to estimate the derivative at that point.
3. Enter `from` and `to` values and click `integral` to estimate the definite integral.
4. In command-line mode, use commands like `derivative x^2 + 3*x at 2` or `integral x^2 from 0 to 3`.

## Supported Calculator Features

* Addition, subtraction, multiplication, and division
* Percent, remainder with `mod`, powers with `^`, and factorial with `!`
* Parentheses and negative numbers
* Square root, cube root, square, reciprocal, and absolute value
* Sine, cosine, tangent, inverse sine, inverse cosine, and inverse tangent
* Natural log, base-10 log, and exponent
* Constants: `pi`, `e`, previous answer with `ans`, and memory with `mem`
* Memory controls: `m+`, `m-`, `ms`, `mr`, and `mc`
* Simple numeric derivatives using `d/dx`
* Simple numeric definite integrals using Simpson's rule

## Development Environment

To recreate the development environment, you need:

* Kotlin command-line tools
* Java runtime with Swing support
* A terminal or command prompt

## Useful Websites to Learn More

* [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
* [Kotlin Math Package](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.math/)

## Future Work

The following items could be improved in the future:

* [ ] Add symbolic derivatives for common expressions
* [ ] Add symbolic integrals for simple polynomial expressions
* [ ] Add graphing for functions that use `x`
