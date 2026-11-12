# ⚒️ CyayoForgeV2 - Advanced Custom Forging System

"Sistem penempaan modular dengan integrasi penuh MMOItems, kalkulasi rentang stat berbasis material, sistem peleburan gear (Crusher), reroll stats, dan sistem pamer item chat."

```text
=========================================
             QUICK TUTORIAL
=========================================
```

### 1. Sistem Penempaan (Forge GUI)
- Buka menu penempaan dengan `/forge` atau `/cyayoforge`.
- Masukkan item **Blueprint** ke slot blueprint (slot kiri).
- Masukkan material yang diminta sesuai kategori ke 3 kolom slot bahan.
- Klik tombol **Tempa (Forge)** untuk menempa item.

### 2. Sistem Peleburan (Crusher GUI)
- Buka menu peleburan dengan `/crusher`.
- Masukkan gear yang memiliki tier sah untuk dihancurkan menjadi **Fragment**.

### 3. Sistem Reroll Senjata (Reroll GUI)
- Buka menu reroll dengan `/reroll`.
- Masukkan senjata/armor hasil tempaan dan fragment yang sesuai dengan tier item.
- Klik tombol **Reroll** untuk mengacak ulang statistik yang memiliki rentang Min-Max tanpa menghilangkan nama penempa, multiplier, maupun kemampuan pasif.

### 4. Sistem Inspeksi & Pamer Item (Chat Showcase)
- Ketik `[weapon]`, `[winspect]`, atau `[ws]` di chat sambil memegang item tempaan untuk membagikan tautan interaktif yang dapat diklik pemain lain.
- Gunakan `/winspect` sambil memegang item untuk melihat rincian penempa, tanggal, kualitas, dan material yang digunakan.

```text
=========================================
           COMMANDS & PERMS
=========================================

USER COMMANDS:
/forge                       > Buka menu penempaan item.
/crusher                     > Buka menu peleburan gear menjadi fragment.
/reroll                      > Buka menu reroll stat senjata/armor.
/winspect                    > Buka menu inspeksi item yang sedang dipegang.
/winspect <id>               > Buka menu inspeksi item showcase dari chat.

ADMIN COMMANDS:
/cyayoforge reload           > Muat ulang seluruh konfigurasi plugin.
/cyayoforge open <player>    > Buka menu forge untuk pemain tertentu.
/cyayoforge crusher <player> > Buka menu crusher untuk pemain tertentu.
/cyayoforge reroll <player>  > Buka menu reroll untuk pemain tertentu.

PERMISSIONS:
cyayoforge.use               > Izin dasar membuka menu /forge, /crusher, /reroll.
cyayoforge.admin             > Akses penuh ke seluruh fitur dan reload (/cyayoforge).
cyayoforge.discount.vipplus  > Diskon 50% biaya reroll item.
cyayoforge.discount.vip      > Diskon 25% biaya reroll item.
```

```text
=========================================
          PLACEHOLDERAPI LIST
=========================================

Gunakan placeholder berikut untuk Scoreboard, Hologram, TAB, Chat, dll:

LEADERBOARD & SKOR:
%cyayoforge_player_score%    > Jumlah senjata 100% Quality yang berhasil ditempa pemain.
%cyayoforge_top_<1-10>_name% > Nama pemain di peringkat Top 1-10 Leaderboard.
%cyayoforge_top_<1-10>_score%> Skor pemain di peringkat Top 1-10 Leaderboard.

INSPEKSI ITEM DI TANGAN (MAIN HAND):
%cyayoforge_hand_is_forged%  > 'true' jika item di tangan adalah hasil tempaan CyayoForge.
%cyayoforge_hand_crafter%    > Nama pembuat / penempa item di tangan.
%cyayoforge_hand_quality%    > Tingkat kesempurnaan (Quality %) item di tangan.
%cyayoforge_hand_multiplier% > Total pengali multiplier material pada item di tangan.
%cyayoforge_hand_dominant%   > Material utama yang digunakan saat menempa item di tangan.
%cyayoforge_hand_rerolls%    > Berapa kali item di tangan telah di-reroll.
```

```text
=========================================
      MMOITEMS LANGUAGE & LORE SETUP
=========================================
```

### 1. Tambahkan ke `plugins/MMOItems/language/stats.yml`:
```yaml
forge-target: '&3 &7■ Target: &e{value}'
forge-requirements: '&3 &7■ Syarat: &f{value}'
forge-bonus-stats: '&3 &7■ Bonus Stat: &a+{value}'
forge-material-multiplier: '&3 &7■ Forge Multiplier: &a+{value}%'
forge-final-multiplier: '&3 &7■ Total Multiplier: &a{value}x'
forge-crafter: '&3 &7■ Penempa: &e{value}'
forge-quality: '&3 &7■ Kesempurnaan: &b{value}%'
forge-dominant-material: '&3 &7■ Material Utama: &f{value}'
forge-reroll-count: '&3 &7■ Reroll: &e{value}'
```

### 2. Tambahkan ke `plugins/MMOItems/language/lore-format.yml`:
```yaml
# Untuk Gear / Senjata / Armor Hasil Tempaan:
- "#forge-quality#"
- "#forge-crafter#"
- "#forge-dominant-material#"
- "#forge-reroll-count#"

# Untuk Blueprint Item:
- "#forge-target#"
- "#forge-requirements#"

# Untuk Material Item:
- "#forge-material-multiplier#"
- "#forge-bonus-stats#"
```

---

Made with ❤️ by **Cyayo**
