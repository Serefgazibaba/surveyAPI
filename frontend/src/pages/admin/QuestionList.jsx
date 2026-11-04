import { useState } from "react";
import Button from "../../components/Button.jsx";
import IconButton from "../../components/IconButton.jsx";
import EmptyState from "../../components/EmptyState.jsx";
import { questionTypeLabel } from "../../utils/statusLabels.js";
import { cloneQuestion } from "../../utils/surveyForm.js";

function QuestionRow({ q, index, total, structural, canEdit, messages, dragging, dropTarget, drag, onEdit, onCopy, onDelete, onMove }) {
  return <>
    <div className={["question-row", messages?.length && "has-error", dragging && "dragging", dropTarget && "drop-target"].filter(Boolean).join(" ")}
      draggable={structural} {...(structural ? drag : {})} id={`question-${index}`}>
      {structural && <span className="grip" title="Sürükleyerek sırala">⠿</span>}
      <span className="question-number">{index + 1}</span>
      <div className="question-copy">
        <b>{q.text || <span className="text-muted">Soru metni girilmedi</span>}{q.required && <em>*</em>}</b>
        {q.options.length > 0 && <span className="question-meta">{q.options.length} seçenek</span>}
      </div>
      <span className={`chip${q.type === "LIKERT" ? " likert" : ""}`}>{questionTypeLabel(q.type)}</span>
      {structural && <>
        <IconButton icon="up" label="Yukarı taşı" disabled={index === 0} onClick={() => onMove(index, index - 1)} />
        <IconButton icon="down" label="Aşağı taşı" disabled={index === total - 1} onClick={() => onMove(index, index + 1)} />
      </>}
      <IconButton icon={canEdit ? "edit" : "eye"} label={canEdit ? "Düzenle" : "Görüntüle"} onClick={onEdit} />
      {structural && <>
        <IconButton icon="copy" label="Kopyala" onClick={onCopy} />
        <IconButton icon="trash" label="Sil" danger onClick={onDelete} />
      </>}
    </div>
    {messages?.map((m, i) => <p key={i} className="question-error">{m}</p>)}
  </>;
}

/**
 * Builder soru listesi (anket ve şablon ortak): sıralama (sürükle / yukarı-aşağı), kopyala, sil, yeni soru.
 * Soru düzenleme modalı sayfada kalır; `onOpen(index | null)` ile açılır.
 * structural: soru ekleme/silme/sıralama serbest mi; canEdit: soru içeriği değiştirilebilir mi.
 */
export default function QuestionList({ questions, setQuestions, structural, canEdit, rowMessages = {}, onOpen }) {
  const [drag, setDrag] = useState({ from: null, over: null });
  const move = (from, to) => setQuestions(list => {
    if (to < 0 || to >= list.length || from === to) return list;
    const next = [...list];
    const [item] = next.splice(from, 1);
    next.splice(to, 0, item);
    return next;
  });
  const dragHandlers = index => ({
    onDragStart: () => setDrag({ from: index, over: null }),
    onDragOver: e => { e.preventDefault(); if (drag.over !== index) setDrag(d => ({ ...d, over: index })); },
    onDrop: e => { e.preventDefault(); move(drag.from, index); setDrag({ from: null, over: null }); },
    onDragEnd: () => setDrag({ from: null, over: null }),
  });

  return <>
    {questions.length === 0 && <EmptyState title="Henüz soru yok" text="İlk sorunuzu ekleyerek başlayın." />}
    {questions.map((q, i) => <QuestionRow key={q.key} q={q} index={i} total={questions.length}
      structural={structural} canEdit={canEdit} messages={rowMessages[i]}
      dragging={drag.from === i} dropTarget={drag.over === i && drag.from !== i} drag={dragHandlers(i)}
      onEdit={() => onOpen(i)} onMove={move}
      onCopy={() => setQuestions(list => [...list.slice(0, i + 1), cloneQuestion(q), ...list.slice(i + 1)])}
      onDelete={() => setQuestions(list => list.filter((_, j) => j !== i))} />)}
    {structural && <Button icon="plus" disabled={questions.length >= 200} onClick={() => onOpen(null)}>Yeni Soru</Button>}
  </>;
}
