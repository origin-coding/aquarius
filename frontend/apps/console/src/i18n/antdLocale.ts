import enUS from "antd/locale/en_US";
import zhCN from "antd/locale/zh_CN";

import { toSupportedLocale } from "@/i18n";

export function getAntdLocale(locale: string | undefined) {
  switch (toSupportedLocale(locale)) {
    case "en-US":
      return enUS;
    case "zh-CN":
      return zhCN;
  }
}
