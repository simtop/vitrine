/*
 * Copyright 2026 simtop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.simtop.vitrine.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

/**
 * Emits the three pieces of generated code each control needs: its remembered state, its UI, and
 * the expression passed to the component.
 *
 * Kept separate from [VitrineProcessor] because it is the part that grows with every new supported
 * type, and separate from [ParameterControl] because it depends on KotlinPoet.
 */
internal object ControlEmitter {

    /**
     * Declares the mutable state backing [name]'s control.
     *
     * Controls that need auxiliary state (a dropdown's expanded flag, a nullable's presence flag)
     * declare it here too, so every `var` in the generated wrapper appears in one block.
     */
    fun emitState(builder: CodeBlock.Builder, name: String, control: ParameterControl) {
        when (control) {
            is ParameterControl.Nullable -> {
                // The inner value is held separately from the presence flag so that toggling a
                // parameter off and back on does not discard what was typed into it.
                emitState(builder, name.valueVar, control.inner)
                builder.rememberVar(name.presentVar, CodeBlock.of("true"))
            }

            is ParameterControl.PreviewParameterValues -> {
                builder.addStatement(
                    "val %N = %M { %T().values.toList() }",
                    name.valuesVal,
                    REMEMBER,
                    ClassName(control.packageName, control.simpleNames),
                )
                builder.rememberVar(name.indexVar, CodeBlock.of("0"))
                builder.rememberVar(name.expandedVar, CodeBlock.of("false"))
            }

            is ParameterControl.EnumDropdown -> {
                builder.rememberVar(
                    name,
                    CodeBlock.of(
                        "%T.%N",
                        ClassName(control.packageName, control.simpleNames),
                        control.entryNames.first(),
                    ),
                )
                builder.rememberVar(name.expandedVar, CodeBlock.of("false"))
            }

            else -> builder.rememberVar(name, initialValue(control))
        }
    }

    /** Emits the control UI for [name]. [label] is what the user sees; it is the parameter name. */
    fun emitControl(
        builder: CodeBlock.Builder,
        name: String,
        label: String,
        control: ParameterControl,
    ) {
        when (control) {
            ParameterControl.TextField -> builder.addStatement(
                "%M(value = %N, onValueChange = { %N = it }, label = { %M(%S) })",
                TEXT_FIELD, name, name, TEXT, label,
            )

            ParameterControl.Checkbox -> {
                builder.beginControlFlow("%M(verticalAlignment = %M.CenterVertically)", ROW, ALIGNMENT)
                builder.addStatement("%M(%S)", TEXT, label)
                builder.addStatement(
                    "%M(checked = %N, onCheckedChange = { %N = it })",
                    CHECKBOX, name, name,
                )
                builder.endControlFlow()
            }

            ParameterControl.IntField -> builder.numberField(name, label, "toIntOrNull", "0")
            ParameterControl.LongField -> builder.numberField(name, label, "toLongOrNull", "0L")

            is ParameterControl.FloatSlider -> builder.slider(
                name, label,
                value = CodeBlock.of("%N", name),
                onChange = CodeBlock.of("it"),
                range = control.range,
            )

            is ParameterControl.DoubleSlider -> builder.slider(
                name, label,
                value = CodeBlock.of("%N.toFloat()", name),
                onChange = CodeBlock.of("it.toDouble()"),
                range = control.range,
            )

            is ParameterControl.DpSlider -> builder.slider(
                name, label,
                value = CodeBlock.of("%N.value", name),
                onChange = CodeBlock.of("it.%M", DP),
                range = control.range,
            )

            ParameterControl.ColorSwatches -> builder.colorSwatches(name, label)

            is ParameterControl.EnumDropdown -> builder.dropdown(
                name = name,
                label = label,
                itemLabels = control.entryNames.map { CodeBlock.of("%S", it) },
                onSelect = { index ->
                    CodeBlock.of(
                        "%N = %T.%N",
                        name,
                        ClassName(control.packageName, control.simpleNames),
                        control.entryNames[index],
                    )
                },
                selectedLabel = CodeBlock.of("%P", "$label: \${$name}"),
                itemCount = control.entryNames.size,
            )

            is ParameterControl.PreviewParameterValues -> builder.providerDropdown(name, label)

            is ParameterControl.Nullable -> {
                builder.beginControlFlow("%M(verticalAlignment = %M.CenterVertically)", ROW, ALIGNMENT)
                builder.addStatement("%M(%P)", TEXT, "$label: not null")
                builder.addStatement(
                    "%M(checked = %N, onCheckedChange = { %N = it })",
                    CHECKBOX, name.presentVar, name.presentVar,
                )
                builder.endControlFlow()
                // Rendering the inner control only when present keeps the UI honest: a null
                // parameter has no value to edit. The state survives regardless.
                builder.beginControlFlow("if (%N)", name.presentVar)
                emitControl(builder, name.valueVar, label, control.inner)
                builder.endControlFlow()
            }
        }
    }

    /** The expression passed for this parameter at the component call site. */
    fun valueExpression(name: String, control: ParameterControl): CodeBlock = when (control) {
        is ParameterControl.Nullable ->
            CodeBlock.of("if (%N) %N else null", name.presentVar, name.valueVar)

        is ParameterControl.PreviewParameterValues ->
            CodeBlock.of("%N[%N]", name.valuesVal, name.indexVar)

        else -> CodeBlock.of("%N", name)
    }

    private fun initialValue(control: ParameterControl): CodeBlock = when (control) {
        ParameterControl.TextField -> CodeBlock.of("%S", "Default Value")
        ParameterControl.Checkbox -> CodeBlock.of("false")
        ParameterControl.IntField -> CodeBlock.of("0")
        ParameterControl.LongField -> CodeBlock.of("0L")
        // Sliders start at their conventional value, pulled inside the range when a narrower one
        // was declared — state outside its own slider's bounds would be passed to the component.
        is ParameterControl.FloatSlider -> CodeBlock.of("%Lf", control.range.start(0f))
        is ParameterControl.DoubleSlider -> CodeBlock.of("%L", control.range.start(0f).toDouble())
        is ParameterControl.DpSlider -> CodeBlock.of("%L.%M", control.range.start(8f), DP)
        ParameterControl.ColorSwatches -> CodeBlock.of("%M.Blue", COLOR)
        // The three composite controls declare their own state in emitState and never reach here.
        is ParameterControl.EnumDropdown,
        is ParameterControl.Nullable,
        is ParameterControl.PreviewParameterValues,
            -> error("${control::class.simpleName} declares its own state")
    }

    private fun CodeBlock.Builder.rememberVar(name: String, initial: CodeBlock) {
        addStatement("var %N by %M { %M(%L) }", name, REMEMBER, MUTABLE_STATE_OF, initial)
    }

    private fun CodeBlock.Builder.numberField(
        name: String,
        label: String,
        parse: String,
        fallback: String,
    ) {
        addStatement(
            "%M(value = %N.toString(), onValueChange = { %N = it.%N() ?: %L }, label = { %M(%S) }, " +
                "keyboardOptions = %M(keyboardType = %M.Number))",
            TEXT_FIELD, name, name, parse, fallback, TEXT, label, KEYBOARD_OPTIONS, KEYBOARD_TYPE,
        )
    }

    private fun CodeBlock.Builder.slider(
        name: String,
        label: String,
        value: CodeBlock,
        onChange: CodeBlock,
        range: SliderRange,
    ) {
        addStatement("%M(%P)", TEXT, "$label: \${$name}")
        addStatement(
            "%M(value = %L, onValueChange = { %N = %L }, valueRange = %Lf..%Lf)",
            SLIDER, value, name, onChange, range.min, range.max,
        )
    }

    private fun CodeBlock.Builder.colorSwatches(name: String, label: String) {
        addStatement("%M(%S)", TEXT, label)
        beginControlFlow("%M(verticalAlignment = %M.CenterVertically)", ROW, ALIGNMENT)
        beginControlFlow(
            "%L.forEach { swatch ->",
            PALETTE.map { CodeBlock.of("%M.%N", COLOR, it) }.joinToCode(prefix = "listOf(", suffix = ")"),
        )
        addStatement(
            "%M(%M.%M(32.%M).%M(swatch).%M(2.%M, if (%N == swatch) %M.Black else %M.Transparent)" +
                ".%M { %N = swatch })",
            BOX, MODIFIER, SIZE, DP, BACKGROUND, BORDER, DP, name, COLOR, COLOR, CLICKABLE, name,
        )
        endControlFlow()
        endControlFlow()
    }

    private fun CodeBlock.Builder.providerDropdown(name: String, label: String) {
        dropdownBox(
            name = name,
            selectedLabel = CodeBlock.of("%P", "$label: \${${name.valuesVal}[${name.indexVar}]}"),
        ) {
            beginControlFlow("%N.forEachIndexed { index, value ->", name.valuesVal)
            addStatement(
                "%M(text = { %M(value.toString()) }, onClick = { %N = index; %N = false })",
                DROPDOWN_MENU_ITEM, TEXT, name.indexVar, name.expandedVar,
            )
            endControlFlow()
        }
    }

    private fun CodeBlock.Builder.dropdown(
        name: String,
        label: String,
        itemLabels: List<CodeBlock>,
        onSelect: (Int) -> CodeBlock,
        selectedLabel: CodeBlock,
        itemCount: Int,
    ) {
        dropdownBox(name, selectedLabel) {
            repeat(itemCount) { index ->
                addStatement(
                    "%M(text = { %M(%L) }, onClick = { %L; %N = false })",
                    DROPDOWN_MENU_ITEM, TEXT, itemLabels[index], onSelect(index), name.expandedVar,
                )
            }
        }
    }

    private fun CodeBlock.Builder.dropdownBox(
        name: String,
        selectedLabel: CodeBlock,
        items: CodeBlock.Builder.() -> Unit,
    ) {
        beginControlFlow("%M", BOX)
        addStatement(
            "%M(onClick = { %N = true }) { %M(%L) }",
            TEXT_BUTTON, name.expandedVar, TEXT, selectedLabel,
        )
        beginControlFlow(
            "%M(expanded = %N, onDismissRequest = { %N = false })",
            DROPDOWN_MENU, name.expandedVar, name.expandedVar,
        )
        items()
        endControlFlow()
        endControlFlow()
    }

    private fun List<CodeBlock>.joinToCode(prefix: String, suffix: String): CodeBlock =
        CodeBlock.builder().apply {
            add(prefix)
            forEachIndexed { index, block ->
                if (index > 0) add(", ")
                add(block)
            }
            add(suffix)
        }.build()

    // Derived state names. They are suffixed rather than mangled because generated code is meant to
    // be read — a catalog is a thing people open.
    private val String.valueVar get() = "${this}Value"
    private val String.presentVar get() = "${this}Present"
    private val String.expandedVar get() = "${this}Expanded"
    private val String.indexVar get() = "${this}Index"
    private val String.valuesVal get() = "${this}Values"

    private val PALETTE = listOf("Blue", "Red", "Green", "Yellow", "Magenta", "Gray", "Black")

    private const val RUNTIME = "androidx.compose.runtime"
    private const val LAYOUT = "androidx.compose.foundation.layout"
    private const val MATERIAL3 = "androidx.compose.material3"
    private const val FOUNDATION = "androidx.compose.foundation"

    private val REMEMBER = MemberName(RUNTIME, "remember")
    private val MUTABLE_STATE_OF = MemberName(RUNTIME, "mutableStateOf")

    private val TEXT = MemberName(MATERIAL3, "Text")
    private val TEXT_FIELD = MemberName(MATERIAL3, "TextField")
    private val CHECKBOX = MemberName(MATERIAL3, "Checkbox")
    private val SLIDER = MemberName(MATERIAL3, "Slider")
    private val TEXT_BUTTON = MemberName(MATERIAL3, "TextButton")
    private val DROPDOWN_MENU = MemberName(MATERIAL3, "DropdownMenu")
    private val DROPDOWN_MENU_ITEM = MemberName(MATERIAL3, "DropdownMenuItem")

    private val ROW = MemberName(LAYOUT, "Row")
    private val BOX = MemberName(LAYOUT, "Box")
    private val SIZE = MemberName(LAYOUT, "size")

    private val BACKGROUND = MemberName(FOUNDATION, "background")
    private val BORDER = MemberName(FOUNDATION, "border")
    private val CLICKABLE = MemberName(FOUNDATION, "clickable")

    private val MODIFIER = MemberName("androidx.compose.ui", "Modifier")
    private val ALIGNMENT = MemberName("androidx.compose.ui", "Alignment")
    private val COLOR = MemberName("androidx.compose.ui.graphics", "Color")
    private val DP = MemberName("androidx.compose.ui.unit", "dp")
    private val KEYBOARD_OPTIONS = MemberName("androidx.compose.foundation.text", "KeyboardOptions")
    private val KEYBOARD_TYPE = MemberName("androidx.compose.ui.text.input", "KeyboardType")
}
