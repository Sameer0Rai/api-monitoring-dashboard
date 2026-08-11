import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { login as loginRequest, registerAccount } from "../api/auth";
import { EMAIL_STORAGE_KEY, TOKEN_STORAGE_KEY } from "../config/env";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [email, setEmail] = useState(() => localStorage.getItem(EMAIL_STORAGE_KEY));

  const persistSession = useCallback((response) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
    localStorage.setItem(EMAIL_STORAGE_KEY, response.email);
    setToken(response.token);
    setEmail(response.email);
  }, []);

  const login = useCallback(
    (emailValue, password) => loginRequest(emailValue, password).then((res) => persistSession(res.data)),
    [persistSession]
  );

  const register = useCallback(
    (emailValue, password) => registerAccount(emailValue, password).then((res) => persistSession(res.data)),
    [persistSession]
  );

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(EMAIL_STORAGE_KEY);
    setToken(null);
    setEmail(null);
  }, []);

  const value = useMemo(
    () => ({ token, email, isAuthenticated: Boolean(token), login, register, logout }),
    [token, email, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
