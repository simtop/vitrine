GRADLE := ./gradlew

.PHONY: help build test sample check publish-local publish clean

help: ## Show this help.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

build: ## Assemble both published artifacts.
	$(GRADLE) :vitrine-annotations:assembleRelease :vitrine-processor:assemble

test: ## Run the processor's unit tests.
	$(GRADLE) :vitrine-processor:test

sample: ## Run the processor over the sample module and compile the generated code.
	$(GRADLE) :sample:assembleRelease

check: ## Run everything CI runs (unit tests + end-to-end generation).
	$(GRADLE) check :sample:assembleRelease

publish-local: ## Publish both artifacts to mavenLocal for dogfooding.
	$(GRADLE) publishToMavenLocal

publish: ## Publish to Maven Central (needs signing + Central credentials).
	$(GRADLE) publishAndReleaseToMavenCentral

clean: ## Delete build outputs.
	$(GRADLE) clean
