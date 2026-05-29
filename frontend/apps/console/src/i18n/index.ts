import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";

import { common as commonEnUS } from "@/i18n/resources/en-US/common";
import { errors as errorsEnUS } from "@/i18n/resources/en-US/errors";
import { menu as menuEnUS } from "@/i18n/resources/en-US/menu";
import { validation as validationEnUS } from "@/i18n/resources/en-US/validation";
import { common as commonZhCN } from "@/i18n/resources/zh-CN/common";
import { errors as errorsZhCN } from "@/i18n/resources/zh-CN/errors";
import { menu as menuZhCN } from "@/i18n/resources/zh-CN/menu";
import { validation as validationZhCN } from "@/i18n/resources/zh-CN/validation";

export const supportedLocales = ["zh-CN", "en-US"] as const;

export type SupportedLocale = (typeof supportedLocales)[number];

export const resources = {
  "zh-CN": {
    common: commonZhCN,
    errors: errorsZhCN,
    menu: menuZhCN,
    validation: validationZhCN,
  },
  "en-US": {
    common: commonEnUS,
    errors: errorsEnUS,
    menu: menuEnUS,
    validation: validationEnUS,
  },
} as const;

export function toSupportedLocale(language: string | undefined): SupportedLocale {
  if (language?.toLowerCase().startsWith("en")) {
    return "en-US";
  }

  return "zh-CN";
}

function applyDocumentLocale(language: string | undefined) {
  document.documentElement.lang = toSupportedLocale(language);
}

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    defaultNS: "common",
    detection: {
      caches: ["localStorage"],
      convertDetectedLanguage: toSupportedLocale,
      lookupLocalStorage: "i18nextLng",
      order: ["localStorage", "navigator"],
    },
    enableSelector: true,
    fallbackLng: "zh-CN",
    interpolation: {
      escapeValue: false,
    },
    ns: ["common", "menu", "validation", "errors"],
    react: {
      useSuspense: false,
    },
    resources,
    supportedLngs: supportedLocales,
  });

applyDocumentLocale(i18n.resolvedLanguage ?? i18n.language);
i18n.on("languageChanged", applyDocumentLocale);

export default i18n;
