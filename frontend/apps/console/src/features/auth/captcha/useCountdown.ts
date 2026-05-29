import { useEffect, useState } from "react";

export function useCountdown(targetTime: number | null): number {
  const [remainingSeconds, setRemainingSeconds] = useState(() =>
    calculateRemainingSeconds(targetTime),
  );

  useEffect(() => {
    setRemainingSeconds(calculateRemainingSeconds(targetTime));

    if (!targetTime) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setRemainingSeconds(calculateRemainingSeconds(targetTime));
    }, 1000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [targetTime]);

  return remainingSeconds;
}

function calculateRemainingSeconds(targetTime: number | null): number {
  if (!targetTime) {
    return 0;
  }

  return Math.max(0, Math.ceil((targetTime - Date.now()) / 1000));
}
