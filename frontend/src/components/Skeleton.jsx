/**
 * Skeleton — composable shimmer placeholders for loading states.
 *
 * Primitives:
 *   <Skeleton.Text  lines={3} />          — text block
 *   <Skeleton.Title />                    — page/section title
 *   <Skeleton.Card />                     — card with header + body
 *   <Skeleton.Avatar size={40} />         — round avatar
 *   <Skeleton.Button />                   — action button
 *   <Skeleton.Stat />                     — stat card (icon + number)
 *   <Skeleton.TranscriptCard />           — history list item
 *   <Skeleton.MetadataRow />              — row of metadata chips
 *
 * Usage:
 *   {loading ? <Skeleton.Card /> : <RealCard data={data} />}
 */

// ── Base block ─────────────────────────────────────────────────────────────────
function Block({ width = '100%', height = 14, style = {} }) {
  return (
    <div
      aria-hidden="true"
      className="skeleton"
      style={{ width, height, borderRadius: 4, ...style }}
    />
  )
}

// ── Text lines ─────────────────────────────────────────────────────────────────
function Text({ lines = 3, gap = 8 }) {
  const widths = ['100%', '85%', '70%', '90%', '60%']
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap }} aria-hidden="true">
      {Array.from({ length: lines }, (_, i) => (
        <Block key={i} width={widths[i % widths.length]} height={14} />
      ))}
    </div>
  )
}

// ── Title ──────────────────────────────────────────────────────────────────────
function Title({ width = '60%' }) {
  return <Block width={width} height={24} style={{ borderRadius: 6 }} />
}

// ── Avatar / circle ────────────────────────────────────────────────────────────
function Avatar({ size = 40 }) {
  return <Block width={size} height={size} style={{ borderRadius: '50%', flexShrink: 0 }} />
}

// ── Button ─────────────────────────────────────────────────────────────────────
function SkeletonButton({ width = 120, fullWidth = false }) {
  return (
    <Block
      width={fullWidth ? '100%' : width}
      height={40}
      style={{ borderRadius: 8 }}
    />
  )
}

// ── Stat card ──────────────────────────────────────────────────────────────────
function Stat() {
  return (
    <div aria-hidden="true" style={{
      padding: '12px 14px',
      border: '1px solid #E5E7EB',
      borderRadius: 8,
      display: 'flex', flexDirection: 'column', gap: 6,
    }}>
      <Block width={80}  height={11} />
      <Block width={60}  height={22} />
      <Block width={50}  height={10} />
    </div>
  )
}

// ── Transcript card (history list item) ───────────────────────────────────────
function TranscriptCard() {
  return (
    <div aria-hidden="true" style={{
      padding: '16px 18px',
      border: '1px solid #E5E7EB',
      borderRadius: 12,
      background: '#fff',
      display: 'flex', flexDirection: 'column', gap: 10,
    }}>
      {/* Status badge + date */}
      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
        <Block width={52} height={18} style={{ borderRadius: 20 }} />
        <Block width={80} height={14} />
      </div>
      {/* Text preview */}
      <Text lines={2} gap={6} />
      {/* Metadata row */}
      <div style={{ display: 'flex', gap: 12 }}>
        {[60, 80, 70, 60].map((w, i) => <Block key={i} width={w} height={12} />)}
      </div>
    </div>
  )
}

// ── Card with header + body ────────────────────────────────────────────────────
function Card({ bodyLines = 4 }) {
  return (
    <div aria-hidden="true" style={{
      padding: '20px',
      border: '1px solid #E5E7EB',
      borderRadius: 12,
      background: '#fff',
      display: 'flex', flexDirection: 'column', gap: 14,
    }}>
      <Title width="50%" />
      <Text lines={bodyLines} />
    </div>
  )
}

// ── Metadata row ───────────────────────────────────────────────────────────────
function MetadataRow({ items = 4 }) {
  const widths = [120, 100, 80, 110, 90, 130]
  return (
    <div aria-hidden="true" style={{
      display: 'grid',
      gridTemplateColumns: `repeat(${items}, 1fr)`,
      gap: 8,
    }}>
      {Array.from({ length: items }, (_, i) => (
        <div key={i} style={{
          padding: '10px 12px',
          background: '#F9FAFB',
          border: '1px solid #F3F4F6',
          borderRadius: 8,
          display: 'flex', flexDirection: 'column', gap: 5,
        }}>
          <Block width={widths[i % widths.length] * 0.7} height={11} />
          <Block width={widths[i % widths.length] * 0.9} height={18} />
        </div>
      ))}
    </div>
  )
}

// ── Page header ────────────────────────────────────────────────────────────────
function PageHeader() {
  return (
    <div aria-hidden="true" style={{ marginBottom: 28 }}>
      <Block width="30%" height={28} style={{ marginBottom: 8, borderRadius: 6 }} />
      <Block width="50%" height={16} />
    </div>
  )
}

// ── Dashboard stats grid ───────────────────────────────────────────────────────
function StatsGrid({ cols = 4 }) {
  return (
    <div aria-hidden="true" style={{
      display: 'grid',
      gridTemplateColumns: `repeat(${cols}, 1fr)`,
      gap: 10,
    }}>
      {Array.from({ length: cols }, (_, i) => <Stat key={i} />)}
    </div>
  )
}

// ── History list ───────────────────────────────────────────────────────────────
function HistoryList({ rows = 5 }) {
  return (
    <div aria-hidden="true" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {Array.from({ length: rows }, (_, i) => <TranscriptCard key={i} />)}
    </div>
  )
}

// ── Transcript detail page ─────────────────────────────────────────────────────
function TranscriptDetail() {
  return (
    <div aria-hidden="true" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Status + id row */}
      <div style={{ display: 'flex', gap: 10 }}>
        <Block width={52} height={18} style={{ borderRadius: 20 }} />
        <Block width={60} height={18} />
      </div>
      {/* Metadata grid */}
      <MetadataRow items={5} />
      {/* Transcript box */}
      <Card bodyLines={6} />
    </div>
  )
}

// ── Named exports ──────────────────────────────────────────────────────────────
const Skeleton = {
  Block, Text, Title, Avatar,
  Button: SkeletonButton, Stat,
  TranscriptCard, Card, MetadataRow,
  PageHeader, StatsGrid, HistoryList, TranscriptDetail,
}

export default Skeleton