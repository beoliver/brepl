default: clean build

SERVER="$(PWD)/server"
CLIENT="$(PWD)/client"

clean:
	bash -c "cd $(CLIENT); make clean"
	bash -c "cd $(SERVER); make clean"

build:
	bash -c "cd $(CLIENT); make build"
	mkdir -p "$(SERVER)/public/cljs-out/"
	cp -r $(CLIENT)/resources/public/* $(SERVER)/public/
	cp $(CLIENT)/target/public/cljs-out/dev-main.js $(SERVER)/public/cljs-out/dev-main.js
	bash -c "cd $(SERVER); make build"
