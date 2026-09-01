import pluginVue from 'eslint-plugin-vue'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
  },
  {
    name: 'app/files-to-ignore',
    ignores: ['**/dist/**', '**/dist-ssr/**', '**/coverage/**', '**/node_modules/**'],
  },
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  {
    name: 'app/atelier-overrides',
    rules: {
      // Atelier: allow explicit any in legacy DTOs / editor canvas; strictness is enforced via tsconfig strict:true
      '@typescript-eslint/no-explicit-any': 'off',
      // unused vars are already checked by vue-tsc (noUnusedLocals/Parameters); avoid duplicate noise
      '@typescript-eslint/no-unused-vars': 'off',
      'vue/multi-word-component-names': 'off',
      'prefer-const': 'off',
    },
  },
  skipFormatting,
)
