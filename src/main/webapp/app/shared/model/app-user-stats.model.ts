export interface IAppUserStats {
  id?: string;
  totalLike?: number | null;
  totalDislike?: number | null;
}

export const defaultValue: Readonly<IAppUserStats> = {};
