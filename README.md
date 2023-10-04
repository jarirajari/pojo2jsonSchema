# Generate Json Schema from Java POJOs
First, generate Java model objects (POJOs) from OpenAPI specification using openapi-generator-cli:
https://github.com/OpenAPITools/openapi-generator
```bash
nvm use 16
npm install -g @openapitools/openapi-generator-cli
openapi-generator-cli generate -i openapiv3.yaml -g jaxrs-spec -o test
ll test/src/gen/java/org/openapitools/model/
```
Then, generate JSON schemas (.json) from these POJOs with this application. In the project directory:

```bash
mvn clean install
java -jar target/p2js.jar \
  --source /home/jari/dev/lowcode-backend-python/http-server/test/src/gen/java/org/openapitools/model/ \
  --target ./models/org/openapitools/model
```
You can find generated schemas (.json) in `models/org/openapitools/*.json
`
