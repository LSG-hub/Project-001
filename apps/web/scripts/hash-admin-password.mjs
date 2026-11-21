// Prints an ADMIN_PASSWORD_HASH and a fresh ADMIN_SESSION_SECRET.
// Usage: npm run admin:hash -w @project001/web   (it asks for the password)
import { randomBytes, scryptSync } from 'node:crypto'
import { createInterface } from 'node:readline/promises'

const rl = createInterface({ input: process.stdin, output: process.stdout })
const password = await rl.question('New admin password (min 12 characters): ')
rl.close()
if (password.length < 12) {
  console.error('Too short. Use at least 12 characters.')
  process.exit(1)
}
const salt = randomBytes(16)
const hash = scryptSync(password, salt, 32)
console.log(`\nADMIN_PASSWORD_HASH=scrypt:${salt.toString('hex')}:${hash.toString('hex')}`)
console.log(`ADMIN_SESSION_SECRET=${randomBytes(32).toString('hex')}`)
