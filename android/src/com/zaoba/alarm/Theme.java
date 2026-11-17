package com.zaoba.alarm;

import android.content.Context;

import java.util.ArrayList;

/**
 * 主题 = 配色 + 卡片形状 + 排版 + 导航样式 + 标题语法。
 * 不只是换色：不同主题下卡片画法、字体形态、章节语法、底栏形态都不同。
 */
public class Theme {

    public static final int STYLE_SOFT = 0;    // 渐变柔光卡（默认）
    public static final int STYLE_PANEL = 1;   // 直角技术面板（PRTS）
    public static final int STYLE_BLOCK = 2;   // 圆角实块 + 描边（终末地）
    public static final int STYLE_STRIPE = 3;  // 左缘发光条（流浪地球）
    public static final int STYLE_PAPER = 4;   // 纯色纸面，无描边无渐变（默认）

    public static final int NAV_PILL = 0;      // 胶囊
    public static final int NAV_UNDERLINE = 1; // 下划线
    public static final int NAV_BLOCK = 2;     // 实心块
    public static final int NAV_SEGMENT = 3;   // 分段格

    public String id;
    public String name;
    public String brand;
    public String brandSub;

    public int bg;
    public int card;
    public int card2;
    public int line;
    public int text;
    public int sub;
    public int dim;
    public int accent;
    public int accent2;
    public int ok;
    public int warn;

    public float cardRadius = 16f;
    public int cardStyle = STYLE_SOFT;
    public int navStyle = NAV_PILL;

    public String timeFace = "sans-serif-light";
    public float timeSize = 54f;
    public String labelFace = "sans-serif";
    public boolean capsLabels = false;

    /** 角标框 / 切角 / 网格 / 编号 */
    public int frameStyle = 0;      // 0 用 cardStyle 的画法，1 角标框，2 切角
    public boolean gridBg = false;
    public int bgStyle = 0;      // 0 纯色, 1 网格, 2 网点纸 + 暗角
    public boolean serifTitle = false;
    public boolean motifs = false;
    public boolean headingBar = false;   // 章节标题：左侧粗色条 + 短横条
    public boolean latinSub = false;     // 页面标题下加一行宽字距大写拉丁
    public boolean titleBar = false;     // 页面标题左侧竖条
    public int titleBarColor = 0;        // 0 = 跟随 accent
    public String fontAsset = null;      // assets 里的中文标题字体
    public int dotColor = 0xFF1A1A1A;
    public float dotStep = 13f;
    public int gridColor = 0xFF12202E;
    public boolean indexed = false;
    public float gridStep = 20f;
    public int frameColor = 0;      // 0 = 跟随 accent
    public String headerLeft = "SYS.OP // TIMETABLE";
    public String headerRight = "ONLINE";

    /** 章节标题前缀，比如 PRTS 的 "// " */
    public String sectionPrefix = "";
    /** 顶部技术状态行（PRTS 那种 ONLINE） */
    public boolean techHeader = false;

    public static final ArrayList<Theme> ALL = new ArrayList<Theme>();

    static {
        Theme def = new Theme();
        def.id = "dawn";
        def.name = "校样";
        def.brand = "早八";
        def.brandSub = "校样";
        def.bg = 0xFF141311;
        def.card = 0xFF1B1917;
        def.card2 = 0xFF211E1B;
        def.line = 0xFF2A2723;
        def.text = 0xFFF5EFE2;
        def.sub = 0xFFA89F8E;
        def.dim = 0xFF6B6459;
        def.accent = 0xFFE2543C;
        def.accent2 = 0xFFC9BFA9;
        def.ok = 0xFF7FB08A;
        def.warn = 0xFFE2543C;
        def.cardRadius = 20f;
        def.cardStyle = STYLE_PAPER;
        def.navStyle = NAV_UNDERLINE;
        def.timeFace = "serif";
        def.timeSize = 58f;
        ALL.add(def);

        Theme end = new Theme();
        end.id = "endfield";
        end.name = "终末地";
        end.brand = "早八";
        end.brandSub = "ENDFIELD";
        end.bg = 0xFF141414;
        end.card = 0xFF1F1F1F;
        end.card2 = 0xFF282828;
        end.line = 0xFF383838;
        end.text = 0xFFFAFAFA;
        end.sub = 0xFFB3B3B3;
        end.dim = 0xFF8A8A8A;
        end.accent = 0xFFFFFA00;
        end.accent2 = 0xFFFAFAFA;
        end.ok = 0xFF00D872;
        end.warn = 0xFFF74040;
        end.cardRadius = 4f;
        end.cardStyle = STYLE_PANEL;
        end.navStyle = NAV_BLOCK;
        end.timeFace = "sans-serif";
        end.timeSize = 52f;
        end.capsLabels = true;
        end.sectionPrefix = "▍";
        end.motifs = true;
        ALL.add(end);

        Theme prts = new Theme();
        prts.id = "prts";
        prts.name = "PRTS";
        prts.brand = "PRTS";
        prts.brandSub = "课表闹钟";
        prts.bg = 0xFF000000;
        prts.card = 0xFF181818;
        prts.card2 = 0xFF1D1F20;
        prts.line = 0xFF313131;
        prts.text = 0xFFF5F5F5;
        prts.sub = 0xFFB1B1B1;
        prts.dim = 0xFF8D8D8D;
        prts.accent = 0xFF18D1FF;
        prts.accent2 = 0xFFFFD800;
        prts.ok = 0xFF2FAC78;
        prts.warn = 0xFFE5484D;
        prts.cardRadius = 0f;
        prts.cardStyle = STYLE_PANEL;
        prts.navStyle = NAV_UNDERLINE;
        prts.timeFace = "monospace";
        prts.timeSize = 46f;
        prts.labelFace = "monospace";
        prts.capsLabels = true;
        prts.sectionPrefix = "// ";
        prts.techHeader = true;
        prts.bgStyle = 2;
        prts.dotColor = 0xFF141414;
        prts.dotStep = 12f;
        prts.serifTitle = true;
        prts.headingBar = true;
        prts.headerLeft = "SIGNAL TRANSMISSION ESTABLISHED";
        prts.headerRight = "PRTS";
        ALL.add(prts);

        Theme earth = new Theme();
        earth.id = "earth";
        earth.name = "流浪地球";
        earth.brand = "早八";
        earth.brandSub = "CN171-11";
        earth.bg = 0xFF060B14;
        earth.card = 0xFF0B1422;
        earth.card2 = 0xFF101B2C;
        earth.line = 0xFF17324C;
        earth.text = 0xFFE6F0FA;
        earth.sub = 0xFF7E9BB8;
        earth.dim = 0xFF4E6683;
        earth.accent = 0xFFD9EAF8;
        earth.accent2 = 0xFFFF4D3D;
        earth.ok = 0xFF6FBF9B;
        earth.warn = 0xFFFF4D3D;
        earth.cardRadius = 0f;
        earth.cardStyle = STYLE_STRIPE;
        earth.frameStyle = 1;
        earth.frameColor = 0xFF4B7FA8;
        earth.gridBg = true;
        earth.bgStyle = 1;
        earth.gridColor = 0xFF0D1B2A;
        earth.gridStep = 18f;
        earth.indexed = true;
        earth.techHeader = true;
        earth.headerLeft = "WANDERING EARTH";
        earth.headerRight = "CN171-11";
        earth.navStyle = NAV_SEGMENT;
        earth.timeFace = "sans-serif-medium";
        earth.timeSize = 50f;
        earth.capsLabels = true;
        earth.sectionPrefix = "▮ ";
        earth.titleBar = true;
        earth.titleBarColor = 0xFFFF4D3D;
        earth.fontAsset = "fonts/ZhiMangXing-Regular.ttf";
        ALL.add(earth);

        Theme con = new Theme();
        con.id = "console";
        con.name = "控制台";
        con.brand = "CONSOLE";
        con.brandSub = "课表闹钟";
        con.bg = 0xFF070A0F;
        con.card = 0xFF0B1017;
        con.card2 = 0xFF101822;
        con.line = 0xFF1B2A38;
        con.text = 0xFFE9EEF2;
        con.sub = 0xFF8FA3B5;
        con.dim = 0xFF56707F;
        con.accent = 0xFFE8B14C;
        con.accent2 = 0xFF6FC3E8;
        con.ok = 0xFF6FBF9B;
        con.warn = 0xFFFF4D3D;
        con.cardRadius = 0f;
        con.frameStyle = 1;
        con.gridBg = true;
        con.bgStyle = 1;
        con.gridColor = 0xFF12202E;
        con.indexed = true;
        con.navStyle = NAV_UNDERLINE;
        con.timeFace = "monospace";
        con.timeSize = 44f;
        con.capsLabels = true;
        con.techHeader = true;
        ALL.add(con);
    }

    private static Theme current;

    public static Theme get(String id) {
        for (Theme t : ALL) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return ALL.get(0);
    }

    public static Theme current(Context c) {
        if (current == null) {
            current = get(Store.get(c).theme);
        }
        return current;
    }

    public static Theme select(Context c, String id) {
        current = get(id);
        return current;
    }
}
