import { createHash } from 'node:crypto'
import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const generatedDirectory = path.join(projectRoot, 'src', 'api', 'generated')

async function snapshot(directory, relativeDirectory = '') {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = []

  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const relativePath = path.join(relativeDirectory, entry.name)
    const absolutePath = path.join(directory, entry.name)

    if (entry.isDirectory()) {
      files.push(...(await snapshot(absolutePath, relativePath)))
      continue
    }

    const content = await readFile(absolutePath)
    files.push([relativePath, createHash('sha256').update(content).digest('hex')])
  }

  return files
}

const before = await snapshot(generatedDirectory)
const generation = spawnSync(
  process.execPath,
  ['./node_modules/orval/dist/bin/orval.js', '--config', 'orval.config.ts'],
  { cwd: projectRoot, stdio: 'inherit' },
)

if (generation.status !== 0) process.exit(generation.status ?? 1)

const after = await snapshot(generatedDirectory)

if (JSON.stringify(before) !== JSON.stringify(after)) {
  console.error(
    'O cliente gerado estava desatualizado. Execute npm run api:generate e versione o resultado.',
  )
  process.exit(1)
}

console.log('O cliente gerado está atualizado.')
