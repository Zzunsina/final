import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../employment-frontend/', import.meta.url));
const localRequire = createRequire(`${root}/package.json`);
const viteRequire = createRequire(localRequire.resolve('vite/package.json'));
const { build } = viteRequire('esbuild');
const output = fileURLToPath(new URL('./signup-ui-check.cjs', import.meta.url));
await build({
  stdin: {
    contents: `
      import assert from 'node:assert/strict';
      import React from 'react';
      import { renderToStaticMarkup } from 'react-dom/server';
      import { MemoryRouter } from 'react-router-dom';
      import { Login } from './src/pages/Auth.jsx';
      function render(state) {
        return renderToStaticMarkup(<MemoryRouter initialEntries={[{pathname:'/login',state}]}><Login /></MemoryRouter>);
      }
      assert(!render(null).includes('회원가입이 완료되었습니다.'));
      const personal = render({signupComplete:true,memberType:'JOB_SEEKER'});
      assert(personal.includes('회원가입이 완료되었습니다.'));
      assert(personal.includes('role="status"'));
      assert(personal.includes('개인 로그인'));
      const company = render({signupComplete:true,memberType:'COMPANY'});
      assert(company.includes('회원가입이 완료되었습니다.'));
      assert(company.includes('기업 로그인'));
      console.log('Signup completion notice and personal/company selection checks passed.');
    `,
    loader: 'jsx', resolveDir: root,
  },
  bundle: true, platform: 'node', format: 'cjs', jsx: 'automatic', outfile: output,
  logLevel: 'error',
});
const result = spawnSync(process.execPath, [output], {encoding:'utf8'});
process.stdout.write(result.stdout);
process.stderr.write(result.stderr);
process.exitCode = result.status ?? 1;
