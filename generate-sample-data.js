const jsf = require('json-schema-faker');
const fs = require('fs');
const $RefParser = require('@apidevtools/json-schema-ref-parser');

try {
  const schemaFilePath = process.argv[2];
  const schemaData = fs.readFileSync(schemaFilePath, 'utf8');
  const schema = JSON.parse(schemaData);
  const sampleData = jsf.generate(schema, { requiredOnly: false, resolveJsonPath: true });
  console.log('Sample data', JSON.stringify(sampleData, null, 3));
  $RefParser.dereference(schema, (err, schema) => {
    //console.log('Dereferenced schema', JSON.stringify(schema, null, 3));
    const filename = 'deferenced.json';
    fs.writeFile(filename, JSON.stringify(schema, null, 3), (err) => {
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

