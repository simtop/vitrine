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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.writeTo

public class VitrineProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        VitrineProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            rawOptions = environment.options,
        )
}

internal class VitrineProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val rawOptions: Map<String, String>,
) : SymbolProcessor {

    /**
     * KSP calls [process] once per round. The provider and its service file must be written exactly
     * once, so we latch after the first round that produced anything: writing them again would
     * throw `FileAlreadyExistsException`, and swallowing that exception is precisely the bug that
     * makes service-file aggregation unreliable.
     */
    private var hasGenerated = false

    // getModuleName() is KspExperimental; it only supplies the default provider class name, and a
    // consumer who sets vitrine.providerName never depends on it.
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasGenerated) return emptyList()

        val annotationName = VITRINE_COMPONENT
        val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        if (symbols.isEmpty()) return emptyList()

        val options = try {
            VitrineOptions.from(rawOptions, resolver.getModuleName().asString())
        } catch (e: IllegalArgumentException) {
            logger.error("Vitrine: ${e.message}")
            return emptyList()
        }

        val items = symbols.mapNotNull { func -> describe(func, annotationName) }
        if (items.isEmpty()) return emptyList()

        items.forEach { generateWrapper(it) }
        generateProvider(items, options)

        hasGenerated = true
        return emptyList()
    }

    private fun describe(func: KSFunctionDeclaration, annotationName: String): ComponentInfo? {
        val annotation = func.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationName
        } ?: return null

        fun argument(name: String): String? =
            annotation.arguments.find { it.name?.asString() == name }?.value as? String

        val containingFile = func.containingFile
        if (containingFile == null) {
            logger.error("Vitrine: @VitrineComponent function has no source file", func)
            return null
        }

        val tab = argument("tab").orEmpty()
        if (tab.isBlank()) {
            logger.error("Vitrine: 'tab' must not be blank", func)
            return null
        }

        val functionName = func.simpleName.asString()
        val explicitName = argument("name").orEmpty()

        return ComponentInfo(
            tab = tab,
            displayName = explicitName.ifEmpty { functionName },
            packageName = func.packageName.asString(),
            functionName = functionName,
            demoContainer = argument("demoContainer").orEmpty(),
            wrapperName = "${functionName}_VitrineWrapper",
            parameters = func.parameters.mapNotNull { param ->
                val paramName = param.name?.asString() ?: return@mapNotNull null
                val control = ControlResolver.resolve(param)
                if (control == null) {
                    // Dropping this silently is how you get generated code that doesn't compile:
                    // the parameter vanishes from the call, so a required one becomes a missing
                    // argument error pointing at generated source. Say so here, against the
                    // component, where the fix is.
                    val typeName = param.type.resolve().declaration.qualifiedName?.asString()
                    if (!param.hasDefault) {
                        logger.error(
                            "Vitrine: parameter '$paramName' of type '$typeName' has no control and " +
                                "no default value, so the generated wrapper cannot call " +
                                "'$functionName'. Give it a default, annotate it with " +
                                "@PreviewParameter to supply values, or use a supported type " +
                                "($SUPPORTED_TYPES).",
                            param,
                        )
                    } else {
                        logger.warn(
                            "Vitrine: parameter '$paramName' of type '$typeName' has no control; " +
                                "the generated wrapper will use its default value.",
                            param,
                        )
                    }
                    return@mapNotNull null
                }
                ParameterInfo(paramName, control)
            },
            sourceFile = containingFile,
        )
    }

    private fun generateWrapper(info: ComponentInfo) {
        val wrapper = FunSpec.builder(info.wrapperName)
            .addAnnotation(ClassName(COMPOSE_RUNTIME, "Composable"))
            .addCode(
                buildCodeBlock {
                    info.parameters.forEach { param ->
                        ControlEmitter.emitState(this, param.name, param.control)
                    }
                    add("\n")
                    beginControlFlow(
                        "%M(modifier = %M.%M(16.%M))",
                        MemberName(LAYOUT, "Column"),
                        MemberName(UI, "Modifier"),
                        MemberName(LAYOUT, "padding"),
                        MemberName(UNIT, "dp"),
                    )
                    info.parameters.forEach { param ->
                        ControlEmitter.emitControl(this, param.name, param.name, param.control)
                        addSpacer(8)
                    }
                    addStatement("%M()", MemberName(MATERIAL3, "HorizontalDivider"))
                    addSpacer(16)
                    addComponentCall(info)
                    endControlFlow()
                }
            )
            .build()

        FileSpec.builder(info.packageName, info.wrapperName)
            .addImport(COMPOSE_RUNTIME, "getValue", "setValue")
            .addFunction(wrapper)
            .build()
            .writeTo(codeGenerator, Dependencies(aggregating = false, info.sourceFile))
    }

    private fun com.squareup.kotlinpoet.CodeBlock.Builder.addSpacer(height: Int) {
        addStatement(
            "%M(modifier = %M.%M($height.%M))",
            MemberName(LAYOUT, "Spacer"),
            MemberName(UI, "Modifier"),
            MemberName(LAYOUT, "height"),
            MemberName(UNIT, "dp"),
        )
    }

    private fun com.squareup.kotlinpoet.CodeBlock.Builder.addComponentCall(info: ComponentInfo) {
        val target = info.demoContainer.ifEmpty { info.functionName }
        val args = com.squareup.kotlinpoet.CodeBlock.builder().apply {
            info.parameters.forEachIndexed { index, param ->
                if (index > 0) add(", ")
                add("%N = %L", param.name, ControlEmitter.valueExpression(param.name, param.control))
            }
        }.build()
        addStatement("%M(%L)", MemberName(info.packageName, target), args)
    }

    private fun generateProvider(items: List<ComponentInfo>, options: VitrineOptions) {
        val itemClass = ClassName(VITRINE_PACKAGE, "VitrineItem")
        val providerInterface = ClassName(VITRINE_PACKAGE, "VitrineProvider")

        val provider = TypeSpec.classBuilder(options.providerClassName)
            .apply { if (options.isInternal) addModifiers(KModifier.INTERNAL) }
            .addSuperinterface(providerInterface)
            .addProperty(
                PropertySpec.builder("items", LIST.parameterizedBy(itemClass))
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer(
                        buildCodeBlock {
                            add("listOf(\n")
                            indent()
                            items.forEach { item ->
                                add(
                                    "%T(tab = %S, name = %S, content = { %M() }),\n",
                                    itemClass, item.tab, item.displayName,
                                    MemberName(item.packageName, item.wrapperName),
                                )
                            }
                            unindent()
                            add(")")
                        }
                    )
                    .build()
            )
            .build()

        // Aggregating: the provider indexes every annotated file, so any change to any of them
        // must invalidate it.
        val sources = items.map { it.sourceFile }.distinct().toTypedArray()
        FileSpec.builder(options.generatedPackage, options.providerClassName)
            .addType(provider)
            .build()
            .writeTo(codeGenerator, Dependencies(aggregating = true, *sources))

        registerService(options, sources)
    }

    /**
     * Registers the generated provider with [java.util.ServiceLoader].
     *
     * An `internal` provider cannot be loaded reflectively from another module, so the service file
     * is only written for a public one — emitting it anyway would produce a `ServiceLoader` entry
     * that fails at runtime with an access error.
     */
    private fun registerService(options: VitrineOptions, sources: Array<KSFile>) {
        if (options.isInternal) {
            logger.warn(
                "Vitrine: ${VitrineOptions.ARG_VISIBILITY}=internal, so no ServiceLoader entry was " +
                    "written. Reference ${options.generatedPackage}.${options.providerClassName} directly."
            )
            return
        }
        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *sources),
            packageName = "",
            fileName = "META-INF/services/$VITRINE_PACKAGE.VitrineProvider",
            extensionName = "",
        ).bufferedWriter().use { writer ->
            writer.write("${options.generatedPackage}.${options.providerClassName}\n")
        }
    }

    private data class ParameterInfo(val name: String, val control: ParameterControl)

    private data class ComponentInfo(
        val tab: String,
        val displayName: String,
        val packageName: String,
        val functionName: String,
        val demoContainer: String,
        val wrapperName: String,
        val parameters: List<ParameterInfo>,
        val sourceFile: KSFile,
    )

    private companion object {
        const val VITRINE_PACKAGE = "io.github.simtop.vitrine"
        const val VITRINE_COMPONENT = "$VITRINE_PACKAGE.VitrineComponent"
        const val COMPOSE_RUNTIME = "androidx.compose.runtime"
        const val LAYOUT = "androidx.compose.foundation.layout"
        const val MATERIAL3 = "androidx.compose.material3"
        const val UI = "androidx.compose.ui"
        const val UNIT = "androidx.compose.ui.unit"
        const val SUPPORTED_TYPES =
            "String, Boolean, Int, Long, Float, Double, Dp, Color, any enum, or a nullable of those"
    }
}
