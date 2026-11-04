import { useLocation, useParams } from "react-router-dom";
import Shell from "../../components/Shell.jsx";
import PageHead from "../../components/PageHead.jsx";
import StateCard from "../../components/StateCard.jsx";
import Button from "../../components/Button.jsx";
import DataTable from "../../components/DataTable.jsx";
import StatusBadge from "../../components/StatusBadge.jsx";
import { formatDateTime } from "../../utils/datetime.js";

const columns = [
  { key: "fullName", header: "Ad Soyad", className: "title-cell" },
  { key: "email", header: "E-posta" },
  { key: "emailStatus", header: "E-posta Durumu", render: r => <StatusBadge kind="email" value={r.emailStatus} /> },
];

/** S-06 · Gönderim onayı */
export default function PublishResultPage() {
  const { id } = useParams();
  const { state } = useLocation();
  const result = state?.result;
  const actions = <>
    <Button to="/surveys">Anketlerime dön</Button>
    <Button variant="primary" icon="mail" to={`/mails?surveyId=${id}`}>Gönderim durumunu izle</Button>
  </>;

  return <Shell>
    <PageHead title="Gönderim Onayı" crumbs={[{ label: "Anketlerim", to: "/surveys" }, { label: state?.title || "Anket" }, { label: "Gönderim" }]} />
    {result
      ? <StateCard tone="success" title="Anket yayınlandı" actions={actions}>
          {result.invitationCount} katılımcıya kişiye özel davet e-postası gönderim kuyruğuna alındı ({formatDateTime(result.publishedAt)}).
          E-postaların durumunu Gönderim Durumu ekranından takip edebilirsiniz.
        </StateCard>
      : <StateCard tone="info" title="Anket yayınlandı" actions={actions}>Davetlerin güncel durumunu Gönderim Durumu ekranından takip edebilirsiniz.</StateCard>}
    {result?.invitations?.length > 0 && <div className="mt">
      <DataTable columns={columns} rows={result.invitations} rowKey={r => r.userId} />
    </div>}
  </Shell>;
}
