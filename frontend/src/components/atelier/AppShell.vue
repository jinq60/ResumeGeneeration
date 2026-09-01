<template>
  <div class="min-h-screen bg-stone flex flex-col">
    <header
      class="h-14 bg-paper border-b border-line sticky top-0 z-10 flex items-center px-6 justify-between"
    >
      <router-link to="/" class="font-newsreader font-medium text-ink tracking-tight"
        >Resume Atelier</router-link
      >
      <nav class="flex gap-6 text-sm text-graphite">
        <router-link to="/workbench" class="hover:text-ink">工作台</router-link>
        <router-link to="/resumes" class="hover:text-ink">我的简历</router-link>
        <router-link to="/templates" class="hover:text-ink">模板</router-link>
      </nav>
      <slot name="actions" />
    </header>
    <main class="flex-1"><slot /></main>
  </div>
</template>
