GRADLE := ./gradlew

.PHONY: help build test sample app api check publish-local publish clean

help: ## Show this help.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

build: ## Assemble both published artifacts.
	$(GRADLE) :vitrine-annotations:assembleRelease :vitrine-processor:assemble

test: ## Run the processor's unit tests.
	$(GRADLE) :vitrine-processor:test

sample: ## Run the processor over the sample module and compile the generated code.
	$(GRADLE) :sample:assembleRelease

app: ## Install the runnable sample catalog on a connected device.
	$(GRADLE) :sample-app:installDebug

api: ## Re-dump the public API after an intentional change.
	$(GRADLE) apiDump

check: ## Run everything CI runs (unit tests + API check + generation + sample app).
	$(GRADLE) check apiCheck :sample:assembleRelease :sample-app:assembleDebug

publish-local: ## Publish both artifacts to mavenLocal for dogfooding.
	$(GRADLE) publishToMavenLocal

publish: ## Upload a staged deployment to Maven Central (needs signing + Central credentials).
	# Staged, not auto-released — matches publish.yml. Releasing is a deliberate click in the
	# Portal UI until the pipeline has proven itself.
	$(GRADLE) publishToMavenCentral

clean: ## Delete build outputs.
	$(GRADLE) clean
