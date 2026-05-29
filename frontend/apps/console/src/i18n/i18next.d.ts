import "i18next";
import { resources } from "@/i18n";

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: "common";
    enableSelector: true;
    resources: (typeof resources)["zh-CN"];
  }
}
