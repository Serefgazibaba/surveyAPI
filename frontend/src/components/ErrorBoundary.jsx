import { Component } from "react";
import StateCard from "./StateCard.jsx";
import Button from "./Button.jsx";

/** Beklenmeyen render hatalarını yakalar; beyaz ekran yerine anlaşılır mesaj gösterir. */
export default class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error("Beklenmeyen hata:", error, info);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return <div className="public-wrap">
      <StateCard tone="error" title="Beklenmeyen bir hata oluştu" actions={
        <Button variant="primary" icon="refresh" onClick={() => window.location.reload()}>Sayfayı yenile</Button>
      }>Sayfayı yenileyerek tekrar deneyin. Sorun sürerse sistem yöneticisine bildirin.</StateCard>
    </div>;
  }
}
