type AuthMotionIconProps = {
  variant: "login" | "logout";
  className?: string;
};

export function AuthMotionIcon({ variant, className }: AuthMotionIconProps) {
  if (variant === "login") {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true" className={`motion-auth-icon ${className ?? ""}`.trim()} fill="none">
        <path
          className="motion-login-door"
          d="M17.25 2a2.73 2.73 0 0 1 1.945.805c.52.52.805 1.21.805 1.945v14.5a2.756 2.756 0 0 1-2.75 2.755H10V20.5h7.25c.69 0 1.25-.56 1.25-1.25V4.75c0-.335-.13-.65-.365-.885-.24-.235-.55-.36-.885-.36H10V2h7.25Z"
          fill="currentColor"
        />
        <g className="motion-login-arrow">
          <path
            d="M6.53 7.215l4.145 4.145c.485.49.485 1.285 0 1.77L6.53 17.275l-1.06-1.06L8.685 13H0v-1.5h8.695L5.47 8.275l1.06-1.06Z"
            fill="currentColor"
          />
        </g>
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={`motion-auth-icon ${className ?? ""}`.trim()} fill="none">
      <path
        className="motion-logout-door"
        d="M5.865 20.135c.24.235.55.365.885.365H14v1.505H6.75a2.73 2.73 0 0 1-1.945-.805A2.73 2.73 0 0 1 4 19.255v-14.5A2.756 2.756 0 0 1 6.75 2H14v1.505H6.75c-.69 0-1.25.56-1.25 1.25V19.25c0 .335.13.65.365.885Z"
        fill="currentColor"
      />
      <g className="motion-logout-arrow">
        <path
          d="M16.53 7.215l4.145 4.145c.485.49.485 1.285 0 1.77l-4.145 4.145-1.06-1.06L18.685 13H10v-1.5h8.695L15.47 8.275l1.06-1.06Z"
          fill="currentColor"
        />
      </g>
    </svg>
  );
}
