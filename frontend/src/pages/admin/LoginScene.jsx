/**
 * Giriş ekranının sol tarafındaki dekoratif 3B sahne: eğik düzlemde yüzen anket kartları (Likert, dağılım,
 * çoklu seçim, katılım). Yalnızca CSS (perspective + keyframes); "hareketi azalt" ayarında animasyon durur.
 * Tamamen görseldir: ekran okuyuculardan gizlenir, tıklanamaz.
 */
export default function LoginScene() {
  return <div className="login-scene-stage" aria-hidden="true">
    <div className="login-scene-rig">
      <div className="login-scene-floor" />

      <div className="login-scene-card" style={{ "--z": "10px", width: 200, left: -150, top: -110 }}>
        <div className="row">
          <span className="num">1</span>
          <span className="line" style={{ width: 120 }} />
        </div>
        <div className="likert">
          {[0, 1, 2, 3, 4].map(i => <span key={i} className={`dot${i === 3 ? " on" : ""}`} />)}
        </div>
      </div>

      <div className="login-scene-card" style={{ "--z": "46px", width: 170, left: -10, top: -80, animationDelay: "-2s" }}>
        <div className="bars">
          {[28, 48, 90, 62, 36].map((h, i) => <span key={i} className={`bar b${i}`} style={{ height: `${h}%` }} />)}
        </div>
        <div className="score">4.2 <small>/ 5</small></div>
      </div>

      <div className="login-scene-card" style={{ "--z": "82px", width: 180, left: -120, top: 20, animationDelay: "-4s" }}>
        <span className="line" style={{ width: 100, marginBottom: 9 }} />
        <div className="checks">
          {[[true, 80], [false, 96], [true, 64]].map(([on, w], i) => <div key={i} className="row">
            <span className={`box${on ? " on" : ""}`} />
            <span className={`line${on ? " strong" : ""}`} style={{ width: w }} />
          </div>)}
        </div>
      </div>

      <div className="login-scene-card" style={{ "--z": "118px", width: 150, left: 30, top: 40, animationDelay: "-1s" }}>
        <div className="rate">%70</div>
        <div className="progress-track"><span style={{ width: "70%" }} /></div>
      </div>
    </div>
  </div>;
}
