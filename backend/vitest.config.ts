import { defineConfig } from "vitest/config";
import { fileURLToPath } from "node:url";

export default defineConfig({
  resolve: {
    // Mirror tsconfig's "@/*" -> "./*" path alias so tests import the same way app code does.
    alias: { "@": fileURLToPath(new URL(".", import.meta.url)) },
  },
  test: {
    environment: "node",
    // Cover both the colocated lib tests (lib/__tests__) and the route/helper tests (test/).
    include: ["test/**/*.test.ts", "lib/**/*.test.ts"],
  },
});
