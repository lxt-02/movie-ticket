import { useState } from 'react';
import { opsApi, type OpsView } from '../../api/endpoints';
import { ErrorBox, useAction } from '../../components/ui';

interface Panel {
  title: string;
  description: string;
  load: () => Promise<OpsView>;
  actions: { text: string; run: () => Promise<OpsView>; danger?: boolean; confirm?: string }[];
}

/**
 * Operations for STAFF and ADMIN (backend: /api/v1/bookings/admin/** and /api/v1/payments/admin/** → ADMIN, STAFF)
 */
export default function OpsPage() {
  const panels: Panel[] = [
    {
      title: 'Payment events DLT (booking-service)',
      description: 'Event thanh toán xử lý lỗi nhiều lần bị chuyển sang payment-events.DLT. Sửa nguyên nhân rồi bấm Replay.',
      load: opsApi.dltStatus,
      actions: [
        { text: 'Replay DLT', run: opsApi.dltReplay, confirm: 'Bắt đầu xử lý lại các event trong DLT?' },
        { text: 'Dừng replay', run: opsApi.dltStop },
      ],
    },
    {
      title: 'Outbox booking-service',
      description: 'Lệnh CREATE_PAYMENT / CANCEL_PAYMENT chờ gửi sang Kafka; dòng FAILED cần đưa lại về PENDING.',
      load: opsApi.bookingOutbox,
      actions: [{ text: 'Gửi lại message FAILED', run: opsApi.bookingOutboxRequeue }],
    },
    {
      title: 'Outbox payment-service',
      description: 'Event PAYMENT_* chờ gửi sang Kafka.',
      load: opsApi.paymentOutbox,
      actions: [{ text: 'Gửi lại message FAILED', run: opsApi.paymentOutboxRequeue }],
    },
    {
      title: 'Demo: giả lập sự cố payment-events',
      description: 'Chỉ chạy khi backend bật booking.demo.chaos-enabled=true.',
      load: opsApi.dltStatus,
      actions: [
        { text: 'Bật lỗi', run: () => opsApi.chaos(true), danger: true, confirm: 'Mọi payment event sẽ lỗi và vào DLT. Tiếp tục?' },
        { text: 'Tắt lỗi', run: () => opsApi.chaos(false) },
      ],
    },
  ];

  return (
    <>
      <h1>Vận hành</h1>
      <p className="muted">Dành cho STAFF và ADMIN.</p>
      <div className="grid-2">
        {panels.map((p) => <OpsPanel key={p.title} panel={p} />)}
      </div>
    </>
  );
}

function OpsPanel({ panel }: { panel: Panel }) {
  const [view, setView] = useState<OpsView>();
  const [message, setMessage] = useState<string>();
  const action = useAction();

  const exec = (fn: () => Promise<OpsView>, msg?: string) =>
    action.run(async () => {
      setView(await fn());
      setMessage(msg);
    });

  return (
    <div className="card">
      <h3>{panel.title}</h3>
      <p className="small muted">{panel.description}</p>
      <div className="row">
        <button className="ghost" disabled={action.busy} onClick={() => exec(panel.load)}>Xem trạng thái</button>
        {panel.actions.map((a) => (
          <button key={a.text} className={a.danger ? 'danger' : ''} disabled={action.busy}
            onClick={() => (!a.confirm || confirm(a.confirm)) && exec(a.run, `${a.text}: OK`)}>
            {a.text}
          </button>
        ))}
      </div>
      {message && <div className="ok small">{message}</div>}
      <ErrorBox error={action.error} />
      {view && <pre className="json">{JSON.stringify(view, null, 2)}</pre>}
    </div>
  );
}
