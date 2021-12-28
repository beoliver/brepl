default: clean build

SERVER="$(PWD)/server"
CLIENT="$(PWD)/client"

clean:
	bash -c "cd $(CLIENT); make clean"
	bash -c "cd $(SERVER); make clean"

build:
	bash -c "cd $(CLIENT); make build"
	mkdir -p "$(SERVER)/public/"
	cp -r $(CLIENT)/build/* $(SERVER)/public/	
	bash -c "cd $(SERVER); make build"
