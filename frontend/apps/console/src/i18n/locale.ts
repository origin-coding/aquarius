import i18n, { toSupportedLocale, type SupportedLocale } from "@/i18n";

export async function changeLocale(locale: SupportedLocale) {
  await i18n.changeLanguage(locale);
  localStorage.setItem("i18nextLng", locale);
}

export function getCurrentLocale(): SupportedLocale {
  return toSupportedLocale(i18n.resolvedLanguage ?? i18n.language);
}
