package org.zenframework.z8.compiler.core;

public interface IToken {
	// literal types
	public static final int NOTHING = 0;
	public static final int EOF = 0;

	public static final int WHITESPACE = 257;
	public static final int LINEBREAK = 258;
	public static final int COMMENT = 259;

	// operators
	public static final int NOT = 260;
	public static final int MUL = 261;
	public static final int DIV = 262;
	public static final int MOD = 263;
	public static final int PLUS = 264;
	public static final int MINUS = 265;

	public static final int EQU = 266;
	public static final int NOT_EQU = 267;
	public static final int LESS = 268;
	public static final int MORE = 269;
	public static final int LESS_EQU = 270;
	public static final int MORE_EQU = 271;
	public static final int AND = 272;
	public static final int OR = 273;
	public static final int GROUP = 274;

	public static final int ADD_ASSIGN = 275;
	public static final int SUB_ASSIGN = 276;
	public static final int MUL_ASSIGN = 277;
	public static final int DIV_ASSIGN = 278;
	public static final int MOD_ASSIGN = 279;

	public static final int LBRACE = 280;
	public static final int RBRACE = 281;
	public static final int LBRACKET = 282;
	public static final int RBRACKET = 283;
	public static final int LCBRACE = 284;
	public static final int RCBRACE = 285;
	public static final int BRACKETS = 286;
	public static final int COLON = 287;
	public static final int SEMICOLON = 288;
	public static final int ASSIGN = 289;
	public static final int QUESTION = 290;
	public static final int COMMA = 291;
	public static final int ELVIS = 292;
	public static final int DOT = 293;

	public static final int CONSTANT = 294;
	public static final int IDENTIFIER = 295;

	// keywords
	public static final int OPERATOR = 296;

	public static final int IF = 297;
	public static final int ELSE = 298;

	public static final int DO = 299;
	public static final int FOR = 300;
	public static final int WHILE = 301;

	public static final int BREAK = 302;
	public static final int RETURN = 303;
	public static final int CONTINUE = 304;

	public static final int THIS = 305;
	public static final int SUPER = 306;
	public static final int CONTAINER = 307;

	public static final int NULL = 308;

	public static final int IMPORT = 309;
	public static final int CLASS = 310;
	public static final int PUBLIC = 311;
	public static final int PROTECTED = 312;
	public static final int PRIVATE = 313;
	public static final int EXTENDS = 314;

	public static final int ENUM = 315;
	public static final int RECORDS = 316;

	public static final int AUTO = 317;

	public static final int NEW = 318;
	public static final int STATIC = 319;

	public static final int TRY = 320;
	public static final int CATCH = 321;
	public static final int FINALLY = 322;
	public static final int THROW = 323;

	public static final int VIRTUAL = 324;
	public static final int FINAL = 325;
	public static final int INSTANCE_OF = 326;

	public int getId();

	public IPosition getPosition();

	public String getRawText();
}
