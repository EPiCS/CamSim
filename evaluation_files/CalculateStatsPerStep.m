function [Mean, Std] = CalculateStatsPerStep(table)
    Mean= mean(table.').';
    Std = std(table.').';
end