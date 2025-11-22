export interface IAppUserStats {
  id?: number;
  totalLike?: number | null;
  totalDislike?: number | null;
}

export const defaultValue: Readonly<IAppUserStats> = {};
