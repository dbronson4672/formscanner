build-FormScannerLayer:
	./build.sh
	mkdir -p "$(ARTIFACTS_DIR)/java/lib"
	cp formscanner-main/target/formscanner.jar "$(ARTIFACTS_DIR)/java/lib/formscanner.jar"
