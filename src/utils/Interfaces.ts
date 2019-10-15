import { Router } from "express";
import Logger from "./Logger";

export interface Settings {
    version: string;
    env: string;
    baseUrl: string;
    userAgent: string;
}

export interface APIOptions {
    settings: Settings,
    logger: Logger
}

export interface Controller {
    router: Router;
    settings: Settings;
}

export interface ConstructionData {
    time: string;
    ships: string[];
}

export interface Names {
    en: string | null;
    cn: string | null;
    jp: string | null;
    kr: string | null;
}

export interface Skin {
    title: string | null;
    image: string | null;
}

export interface Miscellaneous {
    artist: { link: string, name: string } | null,
    web: { link: string, name: string } | null,
    pixiv: { link: string, name: string } | null,
    twitter: { link: string, name: string } | null,
    voiceActress: { link: string, name: string } | null
}