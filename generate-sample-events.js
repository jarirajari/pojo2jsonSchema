const fs = require('fs');
const $RefParser = require('@apidevtools/json-schema-ref-parser');
const jp = require('jsonpath');

let events = [];
let schemaFilePath = process.argv[2]; // dereferenced
try {
  const schemaData = fs.readFileSync(schemaFilePath, 'utf8');
  const schema = JSON.parse(schemaData);
  // In OpenAPI v3 terms, paths are endpoints (resources) and operations are the HTTP methods: e.g. GET and POST
  // A single path can have multiple operations: each operation has requestBody that is under 'responses' (paths>path>operation>responses)
  // Each requestBody can have multiple 'content' types, and each of the types can have its' own schema. (see also anyOf and oneOf).
  // Each operation must have at least one response defined, usually a successful response. A response is defined by its HTTP status code.
  // Again, there can be 'content' types, and each of the types can have its' own schema.
  //
  // Summary, HTTP requests consist of {path, operation, parameters, content, schema} -tuples
  //          HTTP responses consist of {path, operation, responses, content, schema} -tuples
  //
  const httpRequests = jp.nodes(schema, '$.paths.*.*');
  const httpReqSub = httpRequests.map(item => {
    let combinedSchema = {};
    if (item.value.parameters) {
      combinedSchema = {
        someOf: item.value.parameters
      }
    }
    const method = item.path[3];
    const path = item.path[2];
    const req = {
      id: "" + method.toUpperCase() + "[" + path + "]-" + "default",
      type: 'REQ',
      schema: combinedSchema
    };
    events.push(req);
    // console.log('req', JSON.stringify(req_id, null, 3));
    if (item.value.responses) {
      for (const [key, value] of Object.entries(item.value.responses)) {
        let val = {};
        if (value.content && value.content["application/json"]) {
          val = value.content["application/json"].schema;
        }
        const res = {
          id: "" + method.toUpperCase() + "[" + path + "]-" + key,
          type: 'RES',
          schema: val
        };
        events.push(res);
      }
    }
  });
} catch (error) {
  console.error('Error reading or parsing the schema file:', error);
}

const eventsString = JSON.stringify(events, null, 3);
const filePath = "events-"+schemaFilePath

// Write the JSON string to the file
fs.writeFile(filePath, eventsString, 'utf8', (err) => {
  if (err) {
    console.error('Error writing to file:', err);
  } else {
    console.log('Events written to', filePath);
  }
});
