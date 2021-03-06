package me.bristermitten.frigga.runtime.expression

import me.bristermitten.frigga.runtime.FriggaContext
import me.bristermitten.frigga.runtime.Stack
import me.bristermitten.frigga.runtime.entity.Function
import me.bristermitten.frigga.runtime.entity.type.Type

data class FunctionCallExpression(
    private val on: String?,
    private val calls: String,
    private val parameters: List<Expression>
) : Expression {

    private lateinit var function: Function

    override fun initialize(context: FriggaContext) {
        val callingOn = on?.let { context.getProperty(it) }
        val functionProperty = if (callingOn != null) {
            callingOn.type.properties[calls]
        } else {
            context.getProperty(calls)
        } ?: throw IllegalArgumentException("No such function $calls")

        this.function = functionProperty.value as Function
    }

    override fun eval(stack: Stack, context: FriggaContext) {

        val functionStack = Stack()

        val expectedParameters = function.signature.params.entries.toList()
        if (expectedParameters.size != parameters.size) {
            throw IllegalArgumentException("More parameters passed than expected to function $calls. Expected ${expectedParameters.size}, got ${parameters.size}")
        }

        parameters.forEachIndexed { index, it ->
            it.initialize(context)
            if (!expectedParameters[index].value.matches(it.type)) {
                throw IllegalArgumentException("Invalid Parameter")
            }

            it.eval(stack, context) //Push results onto the Stack. May want to restrict push capabilities?
            val pulled = stack.pull()
            context.addParameterValue(expectedParameters[index].key, pulled)
            functionStack.push(pulled)
        }

        function.eval(functionStack, context)
        stack.push(functionStack.pull()) //Push the yielded value onto the main stack
    }

    override val type: Type
        get() = function.signature.returnType
}
