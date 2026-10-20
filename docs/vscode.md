# IntelliJ와 VS Code에서 나누어 실행

IntelliJ에서는 `C:\JobBridge\employment-backend`를 Gradle 프로젝트로 열고 Spring Boot를 실행합니다. 백엔드는 `http://localhost:8080`입니다.

VS Code에서는 `C:\JobBridge\employment-frontend` 폴더를 열고 새 터미널에서 실행합니다.

```powershell
npm run dev
```

브라우저에서 `http://localhost` 또는 `http://localhost:80`을 엽니다. `/api` 요청은 Vite 프록시를 통해 `http://localhost:8080`으로 전달됩니다.

각 IDE에는 담당 폴더만 열면 됩니다. 다른 폴더를 삭제할 필요는 없습니다. 프런트와 백엔드는 실행 중 API로 연결됩니다.

기존 터미널에는 변경된 PATH 설정이 반영되지 않으므로 터미널을 닫고 새로 엽니다. 이 PC에서는 `tools/bin/npm.cmd`가 프로젝트 전용 실제 npm CLI를 실행합니다. 다른 PC에서는 Node.js와 npm 설치 후 프런트 폴더에서 `npm ci`를 한 번 실행합니다.

전체 `C:\JobBridge`를 VS Code에서 열어도 루트의 `npm run dev`로 프런트를 실행할 수 있습니다. IntelliJ에서 백엔드를 실행 중이면 VS Code에서 백엔드를 다시 시작하지 않습니다.

80번 포트가 사용 중이면 Vite가 오류를 표시합니다. 자동으로 다른 포트로 변경하지 않습니다.
