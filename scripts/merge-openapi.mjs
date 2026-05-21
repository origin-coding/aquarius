import { readdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { isDeepStrictEqual } from "node:util";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const contractsDir = path.join(repoRoot, "contracts");
const generatedDir = path.join(contractsDir, "generated");
const manualDir = path.join(contractsDir, "manual");
const outputFile = path.join(contractsDir, "openapi.json");

const HTTP_METHODS = new Set([
  "delete",
  "get",
  "head",
  "options",
  "patch",
  "post",
  "put",
  "trace",
]);

const readJsonDocuments = async (dir) => {
  let entries;

  try {
    entries = await readdir(dir, { withFileTypes: true });
  } catch (error) {
    if (error.code === "ENOENT") {
      return [];
    }
    throw error;
  }

  const files = entries
    .filter((entry) => entry.isFile() && entry.name.endsWith(".json"))
    .map((entry) => path.join(dir, entry.name))
    .sort((left, right) => left.localeCompare(right));

  return Promise.all(
    files.map(async (file) => ({
      file,
      document: JSON.parse(await readFile(file, "utf8")),
    })),
  );
};

const clone = (value) => JSON.parse(JSON.stringify(value));

const addUniqueByJson = (target, values = []) => {
  for (const value of values) {
    if (!target.some((existing) => isDeepStrictEqual(existing, value))) {
      target.push(clone(value));
    }
  }
};

const mergeNamedObjects = (target, source = {}, sourceFile, section) => {
  for (const [name, value] of Object.entries(source)) {
    if (target[name] === undefined) {
      target[name] = clone(value);
      continue;
    }

    if (!isDeepStrictEqual(target[name], value)) {
      throw new Error(`Conflicting ${section} "${name}" in ${sourceFile}`);
    }
  }
};

const mergeComponents = (target, source = {}, sourceFile) => {
  for (const [section, entries] of Object.entries(source)) {
    if (target[section] === undefined) {
      target[section] = clone(entries);
      continue;
    }

    mergeNamedObjects(target[section], entries, sourceFile, `components.${section}`);
  }
};

const mergePathItem = (target, source, sourceFile, route) => {
  for (const [key, value] of Object.entries(source)) {
    if (HTTP_METHODS.has(key.toLowerCase()) && target[key] !== undefined) {
      throw new Error(`Conflicting operation "${key.toUpperCase()} ${route}" in ${sourceFile}`);
    }

    if (target[key] === undefined) {
      target[key] = clone(value);
      continue;
    }

    if (!isDeepStrictEqual(target[key], value)) {
      throw new Error(`Conflicting path item "${route}.${key}" in ${sourceFile}`);
    }
  }
};

const mergePaths = (target, source = {}, sourceFile) => {
  for (const [route, pathItem] of Object.entries(source)) {
    if (target[route] === undefined) {
      target[route] = clone(pathItem);
      continue;
    }

    mergePathItem(target[route], pathItem, sourceFile, route);
  }
};

const mergeOpenApiDocument = (target, source, sourceFile) => {
  if (source.openapi && target.openapi && source.openapi !== target.openapi) {
    throw new Error(`OpenAPI version mismatch in ${sourceFile}: ${source.openapi} != ${target.openapi}`);
  }

  target.openapi ??= source.openapi;
  target.info ??= clone(source.info ?? {});
  target.paths ??= {};
  target.components ??= {};

  addUniqueByJson(target.servers ??= [], source.servers);
  addUniqueByJson(target.tags ??= [], source.tags);
  mergePaths(target.paths, source.paths, sourceFile);
  mergeComponents(target.components, source.components, sourceFile);

  for (const key of ["externalDocs", "security", "webhooks"]) {
    if (source[key] === undefined) {
      continue;
    }

    if (target[key] === undefined) {
      target[key] = clone(source[key]);
      continue;
    }

    if (!isDeepStrictEqual(target[key], source[key])) {
      throw new Error(`Conflicting top-level "${key}" in ${sourceFile}`);
    }
  }
};

const generatedDocuments = await readJsonDocuments(generatedDir);
const manualDocuments = await readJsonDocuments(manualDir);

if (generatedDocuments.length === 0) {
  throw new Error(`No generated OpenAPI documents found in ${generatedDir}`);
}

const [{ document: baseDocument }, ...remainingGeneratedDocuments] = generatedDocuments;
const mergedDocument = clone(baseDocument);

for (const { file, document } of [...remainingGeneratedDocuments, ...manualDocuments]) {
  mergeOpenApiDocument(mergedDocument, document, path.relative(repoRoot, file));
}

await writeFile(outputFile, `${JSON.stringify(mergedDocument, null, 2)}\n`);

console.log(
  `Merged ${generatedDocuments.length} generated and ${manualDocuments.length} manual OpenAPI document(s) into ${path.relative(repoRoot, outputFile)}`,
);
