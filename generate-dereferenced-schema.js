const fs = require('fs');
const $RefParser = require('@apidevtools/json-schema-ref-parser');
const jp = require('jsonpath');

try {
  const schemaFilePath = process.argv[2];
  const schemaData = fs.readFileSync(schemaFilePath, 'utf8');
  const schema = JSON.parse(schemaData);
  const processed = JSON.stringify(schema, null, 3);
  $RefParser.dereference(schema, (err, drs) => {
    console.log('Dereferencing schema');
    const filename = 'deferenced.json';
    fs.writeFile(filename, JSON.stringify(drs, null, 3), (err) => {
      if (err) {
        console.error('Error writing to the file:', err);
      } else {
        console.log(`Output written to ${filename}`);
      }
    });
  });
} catch (error) {
  console.error('Error reading or parsing the schema file:', error);
}

